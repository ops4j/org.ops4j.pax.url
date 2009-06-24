/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.url.manifest.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.manifest.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for "manifest:" protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, June 24, 2009
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
            new String[]{
                ServiceConstants.PROTOCOL_MANIFEST,
                ServiceConstants.PROTOCOL_MF
            },
            ServiceConstants.PID,
            new ConnectionFactory<Configuration>()
            {

                /**
                 * Creates a "manifest" url connection.
                 *
                 * @see ConnectionFactory#createConection(BundleContext, URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final Configuration config )
                    throws IOException
                {
                    return createConnection( url, config );
                }

                /**
                 * @see ConnectionFactory#createConfiguration(PropertyResolver)
                 */
                public Configuration createConfiguration( final PropertyResolver propertyResolver )
                {
                    return new ConfigurationImpl( propertyResolver );
                }

            }
        );
    }

    /**
     * Creates a "manifest" url connection.
     *
     * @param url    manifest file url
     * @param config configuration
     *
     * @return url connection
     *
     * @throws IOException re-thrown
     */
    public static URLConnection createConnection( final URL url,
                                                  final Configuration config )
        throws IOException
    {
        final String protocol = url.getProtocol();
        if( ServiceConstants.PROTOCOL_MANIFEST.equals( protocol )
            || ServiceConstants.PROTOCOL_MF.equals( protocol ) )
        {
            return new Connection( url, config );
        }
        throw new MalformedURLException( "Unsupported protocol: " + protocol );
    }

}