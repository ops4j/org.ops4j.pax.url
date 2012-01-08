/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.commons.handler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for protocol handlers.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 13, 2007
 */
public class HandlerActivator<T>
    implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( HandlerActivator.class );

    /**
     * Array of handled protocols.
     */
    private final String[] m_protocols;
    /**
     * Service PID.
     */
    private final String m_pid;
    /**
     * Protocol specific connection factory.
     */
    private final ConnectionFactory<T> m_connectionFactory;

    /**
     * Bundle context in use.
     */
    private BundleContext m_bundleContext;
    /**
     * Property resolver to be used on resolving properties.
     */
    private PropertyResolver m_propertyResolver;
    /**
     * Protocol handler specific configuration.
     */
    private T m_configuration;
    /**
     * Handler service registration. Usef for cleanup.
     */
    private ServiceRegistration m_handlerReg;
    /**
     * Managed service registration. Used for cleanup.
     */
    private ServiceRegistration m_managedServiceReg;

    /**
     * Creates a protocol handler.
     *
     * @param protocols         array of handled protocols. Cannot be null.
     * @param pid               service pid. Cannot be null.
     * @param connectionFactory protocol specific connection factory. Cannot be null
     *
     * @throws NullArgumentException if any of the paramters is null
     */
    public HandlerActivator( final String[] protocols,
                             final String pid,
                             final ConnectionFactory<T> connectionFactory )
    {
        NullArgumentException.validateNotNull( protocols, "Protocols" );
        NullArgumentException.validateNotNull( pid, "PID" );
        NullArgumentException.validateNotNull( connectionFactory, "Connection factory" );
        m_protocols = protocols;
        m_pid = pid;
        m_connectionFactory = connectionFactory;
    }

    /**
     * Registers Handler as a wrap: protocol stream handler service and as a configuration managed service if
     * possible.
     *
     * @param bundleContext the bundle context.
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start( final BundleContext bundleContext )
    {
        NullArgumentException.validateNotNull( bundleContext, "Bundle context" );
        m_bundleContext = bundleContext;
        registerManagedService();
        LOG.debug( "Handler for protocols " + Arrays.deepToString( m_protocols ) + " started" );
    }

    /**
     * Performs cleanup:<br/>
     * * Unregister handler;<br/>
     * * Unregister managed service;<br/>
     * * Release bundle context.
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop( final BundleContext bundleContext )
    {
        NullArgumentException.validateNotNull( bundleContext, "Bundle context" );
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
        m_bundleContext = null;
        LOG.debug( "Handler for protocols " + Arrays.deepToString( m_protocols ) + " stopped" );
    }

    /**
     * Register the handler service.
     */
    private void registerHandler()
    {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put( URLConstants.URL_HANDLER_PROTOCOL, m_protocols );
        m_handlerReg = m_bundleContext.registerService(
            URLStreamHandlerService.class.getName(),
            new Handler(),
            props
        );

    }

    /**
     * Registers a managed service to listen on configuration updates.
     */
    private void registerManagedService()
    {
        try
        {
            m_managedServiceReg = OptionalConfigAdminHelper.registerManagedService( m_bundleContext, m_pid, this );
        }
        catch ( Throwable ignore )
        {
            setResolver( new BundleContextPropertyResolver( m_bundleContext ) );
            m_managedServiceReg = null;
        }
    }

    /**
     * Getter.
     *
     * @return property resolver
     */
    synchronized PropertyResolver getResolver()
    {
        return m_propertyResolver;
    }

    /**
     * Setter.
     *
     * @param propertyResolver property resolver
     */
    synchronized void setResolver( final PropertyResolver propertyResolver )
    {
        m_propertyResolver = propertyResolver;
        m_configuration = m_connectionFactory.createConfiguration( propertyResolver );
        if ( m_configuration != null && m_handlerReg == null )
        {
            registerHandler();
        }
        else if ( m_configuration == null && m_handlerReg != null )
        {
            m_handlerReg.unregister();
            m_handlerReg = null;
        }
    }

    /**
     * OSGi URLStreamHandlerService implementation that handles wrap protocol.
     *
     * @author Alin Dreghiciu
     * @since 0.1.0, January 13, 2008
     */
    private class Handler
        extends AbstractURLStreamHandlerService
    {

        /**
         * @see org.osgi.service.url.URLStreamHandlerService#openConnection(java.net.URL)
         */
        @Override
        public URLConnection openConnection( final URL url )
            throws IOException
        {
            return m_connectionFactory.createConection( m_bundleContext, url, m_configuration );
        }

    }


}