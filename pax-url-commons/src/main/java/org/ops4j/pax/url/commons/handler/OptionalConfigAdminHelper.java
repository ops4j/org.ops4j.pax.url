package org.ops4j.pax.url.commons.handler;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Hashtable;

class OptionalConfigAdminHelper {
    private OptionalConfigAdminHelper() {
        // utility class
    }

    static ServiceRegistration registerManagedService(
            final String m_pid, final BundleContext m_bundleContext, final HandlerActivator<?> parent) {
        final ManagedService managedService = new ManagedService() {
            /**
             * Sets the resolver on handler.
             *
             * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
             */
            public void updated(@SuppressWarnings("rawtypes") final Dictionary config)
                    throws ConfigurationException {
                if (config == null) {
                    parent.setResolver(new BundleContextPropertyResolver(m_bundleContext));
                } else {
                    parent.setResolver(new DictionaryPropertyResolver(config));
                }
            }

        };
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, m_pid);
        return m_bundleContext.registerService(
                ManagedService.class.getName(),
                managedService,
                props
        );
    }
}
