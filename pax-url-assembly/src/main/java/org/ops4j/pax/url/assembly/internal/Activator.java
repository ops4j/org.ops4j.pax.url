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
package org.ops4j.pax.url.assembly.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.assembly.ServiceConstants;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for "assembly:" protocol handler.
 * Registers the "assembly:" url handler as a service.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
public final class Activator
    extends HandlerActivator<Void>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL, ServiceConstants.PROTOCOL_REFERENCE },
            ServiceConstants.PID,
            new ConnectionFactory<Void>()
            {

                /**
                 * Creates an "assembly:" url connection.
                 *
                 * @see ConnectionFactory#createConection(BundleContext, java.net.URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final Void notUsed )
                    throws IOException
                {
                    return createConnection( url );
                }

                /**
                 * Returns null, as "assembly:" url handler does nto uses configurations.
                 * @see ConnectionFactory#createConfiguration(PropertyResolver)
                 */
                public Void createConfiguration( final PropertyResolver propertyResolver )
                {
                    return null;
                }

            }
        );
    }

    /**
     * Creates an "assembly:" url connection.
     *
     * @param url assembly url
     *
     * @return url connection
     *
     * @throws IOException re-thrown
     */
    public static URLConnection createConnection( final URL url )
        throws IOException
    {
        if( ServiceConstants.PROTOCOL_REFERENCE.equals( url.getProtocol() ) )
        {
            return new Connection( url, new AssemblyDescriptorUrlParser( url.getPath() ) );
        }
        return new Connection( url, new DirectoryUrlParser( url.getPath() ) );
    }

}