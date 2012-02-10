/*
 * Copyright 2011 Harald Wellmann
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
package org.ops4j.pax.url.reference.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.ops4j.pax.url.api.ArtifactProvider;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.reference.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator for "reference:" protocol handler.
 *
 * @author Harald Wellmann (harald.wellmann@gmx.de)
 * @since 1.3.5, Aug 5, 2011
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
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<Void>()
            {

                /**
                 * Creates a "link" url connection.
                 *
                 * @see ConnectionFactory#createConection(BundleContext, URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final Void notUsed )
                    throws IOException
                {
                    return new ReferenceUrlConnection(url);
                }

                /**
                 * @see ConnectionFactory#createConfiguration(PropertyResolver)
                 */
                public Void createConfiguration( final PropertyResolver propertyResolver )
                {
                    return null;
                }

                public ArtifactProvider apiProvider()
                {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            }
        );
    }
}