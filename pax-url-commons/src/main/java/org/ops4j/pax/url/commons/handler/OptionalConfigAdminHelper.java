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

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config admin helper methods. By being in another class eventual NoClassDefFoundError can be catch and handled.
 *
 * @author Alin Dreghiciu
 * @since 1.1.1, September 06, 2007
 */
class OptionalConfigAdminHelper
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(OptionalConfigAdminHelper.class);

    private OptionalConfigAdminHelper()
    {
        // utility class
    }

    /**
     * Registers a managed service to listen on configuration updates.
     *
     * @param bundleContext bundle context to be used for registration
     * @param pid PID to be used for registration
     * @param handlerActivator handler activator doing registration
     *
     * @return service registration of registered service
     */
    static ServiceRegistration registerManagedService(final BundleContext bundleContext,
            final String pid,
            final HandlerActivator<?> handlerActivator)
    {
        final ManagedService managedService = new ManagedService()
        {
            /**
             * Sets the resolver on handler.
             *
             * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
             */
            public void updated(final Dictionary config)
                throws ConfigurationException
            {
                if (config == null)
                {
                    handlerActivator.setResolver(new BundleContextPropertyResolver(bundleContext));
                }
                else
                {
                    handlerActivator.setResolver(new DictionaryPropertyResolver(config));
                }
            }

        };
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, pid);
        ServiceRegistration registration = bundleContext.registerService(
            ManagedService.class.getName(),
            managedService,
            props
            );
        synchronized ( handlerActivator )
        {
            if ( handlerActivator.getResolver() == null )
            {
                try
                {
                    managedService.updated( null );
                }
                catch ( ConfigurationException ignore )
                {
                    // this should never happen
                    LOG.error( "Internal error. Cannot set initial configuration resolver.", ignore );
                }
            }
        }
        return registration;
    }
}
