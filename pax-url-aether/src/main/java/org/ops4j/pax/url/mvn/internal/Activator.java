/*
 * Copyright 2014 Guillaume Nodet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for protocol handlers.
 */
public class Activator extends AbstractURLStreamHandlerService
        implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( Activator.class );

    /**
     * Bundle context in use.
     */
    private BundleContext m_bundleContext;
    /**
     * Handler service registration. Used for cleanup.
     */
    private ServiceRegistration<URLStreamHandlerService> m_handlerReg;
    /**
     * Managed service registration. Used for cleanup.
     */
    private ServiceRegistration<ManagedService> m_managedServiceReg;
    /**
     * Maven resolver.
     */
    private final AtomicReference<MavenResolver> m_resolver = new AtomicReference<MavenResolver>();
    /**
     * Managed service registration. Used for cleanup.
     */
    private final AtomicReference<ServiceRegistration<MavenResolver>> m_resolverReg =
            new AtomicReference<ServiceRegistration<MavenResolver>>();

    /**
     * Registers Handler as a wrap: protocol stream handler service and as a configuration managed service if
     * possible.
     *
     * @param bundleContext the bundle context.
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start( final BundleContext bundleContext )
    {
        m_bundleContext = bundleContext;
        try {
            updated(null);
        } catch (AssertionError e) {
            LOG.error("Unable to load MavenConfiguration '{}' : '{}'", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "", e);
        }
        registerManagedService();
    }

    /**
     * Performs cleanup:<br/>
     * * Unregister handler;<br/>
     * * Unregister managed service;<br/>
     * * Release bundle context.
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop( final BundleContext bundleContext )
    {
        if ( m_handlerReg != null )
        {
            m_handlerReg.unregister();
            m_handlerReg = null;
        }
        if ( m_managedServiceReg != null )
        {
            m_managedServiceReg.unregister();
            m_managedServiceReg = null;
        }
        ServiceRegistration<MavenResolver> registration = m_resolverReg.getAndSet( null );
        if ( registration != null )
        {
            registration.unregister();
        }
        MavenResolver resolver = m_resolver.getAndSet( null );
        if ( resolver != null )
        {
            try {
                resolver.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        m_bundleContext = null;
        LOG.debug( "Handler for protocols " + ServiceConstants.PROTOCOL + " stopped" );
    }

    /**
     * Register the handler service.
     */
    private void registerHandler()
    {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put( URLConstants.URL_HANDLER_PROTOCOL, ServiceConstants.PROTOCOL );
        m_handlerReg = safeRegisterService(
                URLStreamHandlerService.class,
                this,
                props);
    }

    /**
     * Registers a managed service to listen on configuration updates.
     */
    private void registerManagedService()
    {
        try
        {
            m_managedServiceReg = OptionalConfigAdminHelper.registerManagedService( this );
        }
        catch ( Throwable ignore )
        {
            updated( null );
            m_managedServiceReg = null;
        }
    }

    public void updated(Dictionary<String, ?> config) {
        PropertyResolver propertyResolver;
        if (config == null) {
            propertyResolver = new PropertyResolver() {
                @Override
                public String get(String propertyName) {
                    return m_bundleContext.getProperty(propertyName);
                }
            };
        } else {
            propertyResolver = new DictionaryPropertyResolver(config);
        }
        MavenConfiguration mavenConfig = new MavenConfigurationImpl(propertyResolver, ServiceConstants.PID);
        if (!((MavenConfigurationImpl) mavenConfig).isValid()) {
             return;
        }
        MavenResolver resolver = new AetherBasedResolver(mavenConfig);
        MavenResolver oldResolver = m_resolver.getAndSet( resolver );
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("configuration", config == null ? "bundlecontext" : "configadmin");
        ServiceRegistration<MavenResolver> registration = safeRegisterService(
                MavenResolver.class,
                resolver,
                properties);
        registration = m_resolverReg.getAndSet(registration);
        if (registration != null) {
            registration.unregister();
        }
        if ( oldResolver != null ) {
            try {
                oldResolver.close();
            } catch (IOException e) {
                // Ignore
            }
        } else {
            // first registration of URLStreamHandlerService
            registerHandler();
        }
    }

    @Override
    public URLConnection openConnection( final URL url )
            throws IOException
    {
        return new Connection( url, m_resolver.get() );
    }

    /**
     * Safely registers the specified service to the {@link BundleContext}.
     * <p>
     * This method is introduced to support backward compatibility with the older 4.0.0
     * version because org.osgi.framework in version 1.5 has method
     * <code>safeRegisterService(String,Object,Dictionary)</code> while in version
     * 1.6 it is changed to <code>safeRegisterService(String,Object,Dictionary<String, ? >)</code>.
     * The dictionary is not a raw typed.
     * <p>
     * The method can be replaced to the single <code>registerService</code> method
     * invocation if the backward compatibility will not be supported any more. In
     * such case the specified version ranges in <i>osgi.bnd</i> should be replaced
     * or removed as well: <pre>
     * {@code
     * org.osgi.framework;version="[1.5,2)"
     * org.osgi.util.tracker;version="[1.4,2)" }</pre>
     */
    @SuppressWarnings("unchecked")
    private <T> ServiceRegistration<T> safeRegisterService(Class<T> clazz, T service, Dictionary<String, ?> properties) {
        Method[] methods = BundleContext.class.getMethods();
        for (Method method : methods) {
            Class<?>[] params = method.getParameterTypes();
            if ("registerService".equals(method.getName())
                    && params.length == 3
                    && String.class.equals(params[0])
                    && Object.class.equals(params[1])
                    && Dictionary.class.equals(params[2])) {
                try {
                    return (ServiceRegistration<T>) method.invoke(
                            m_bundleContext,
                            clazz.getName(),
                            service,
                            properties);
                } catch (Exception e) {
                    LOG.error("Unable to register service {}", service, e);
                    return null;
                }
            }
        }
        LOG.error("Method registerService is not found in BundleContext");
        return null;
    }

    static class OptionalConfigAdminHelper {

        /**
         * Registers a managed service to listen on configuration updates.
         */
        static ServiceRegistration<ManagedService> registerManagedService(final Activator activator)
        {
            final Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(Constants.SERVICE_PID, ServiceConstants.PID);
            return activator.safeRegisterService(
                    ManagedService.class,
                    new ManagedService() {
                        @Override
                        public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
                            try {
                                activator.updated(dictionary);
                            } catch (AssertionError e) {
                                LOG.error("Unable to reload MavenConfiguration '{}' : '{}'", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "", e);
                                throw new ConfigurationException("", "", e.getCause());
                            }
                        }
                    },
                    props
            );
        }

    }

}