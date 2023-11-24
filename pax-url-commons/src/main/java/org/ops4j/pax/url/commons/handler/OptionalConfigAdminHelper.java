/*
 * Copyright 2023 OPS4J.
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
package org.ops4j.pax.url.commons.handler;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Hashtable;

class OptionalConfigAdminHelper {
    private OptionalConfigAdminHelper() {
        // utility class
    }

    static ServiceRegistration<ManagedService> registerManagedService(
            final String m_pid, final BundleContext m_bundleContext, final HandlerActivator<?> parent) {
        final ManagedService managedService = new ManagedService() {
            /**
             * Sets the resolver on handler.
             *
             * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
             */
            public void updated(final Dictionary<String, ?> config) {
                if (config == null) {
                    parent.setResolver(new BundleContextPropertyResolver(m_bundleContext));
                } else {
                    parent.setResolver(new DictionaryPropertyResolver(config));
                }
            }
        };
        final Dictionary<String, String> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, m_pid);
        return m_bundleContext.registerService(ManagedService.class, managedService, props
        );
    }
}
