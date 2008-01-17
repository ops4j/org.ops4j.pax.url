/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.url.wrap.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.commons.resolver.Resolver;
import org.ops4j.pax.url.wrap.ServiceConstants;

/**
 * Bundle activator for wrap: protocol handler.
 * TODO add unit tests
 *
 * @author Alin Dreghiciu
 * @since September 09, 2007
 */
public final class Activator
    extends HandlerActivator<Configuration>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<Configuration>()
            {

                /**
                 * @see org.ops4j.pax.url.commons.handler.ConnectionFactory#createConection(BundleContext, URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final Configuration config )
                    throws MalformedURLException
                {
                    return new Connection( url, config );
                }

                /**
                 * @see org.ops4j.pax.url.commons.handler.ConnectionFactory#createConfiguration(Resolver)
                 */
                public Configuration createConfiguration( final Resolver resolver )
                {
                    return new ConfigurationImpl( resolver );
                }

            }
        );
    }

}
