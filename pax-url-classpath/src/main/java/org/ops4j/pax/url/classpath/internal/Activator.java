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
package org.ops4j.pax.url.classpath.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.api.ArtifactProvider;
import org.ops4j.pax.url.classpath.ServiceConstants;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for classpath: protocol handler.
 *
 * @author Alin Dreghiciu
 * @since August 07, 2007
 */
public final class Activator
    extends HandlerActivator<Void>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, org.ops4j.pax.url.commons.handler.ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<Void>()
            {

                /**
                 * Creates a classpath url connection.
                 *
                 * @see org.ops4j.pax.url.commons.handler.ConnectionFactory#createConnection(BundleContext, URL, Object)
                 */
                public URLConnection createConnection( final BundleContext bundleContext,
                                                       final URL url,
                                                       final Void notUsed )
                    throws MalformedURLException
                {
                    return new Connection( url, bundleContext );
                }

                /**
                 * @see org.ops4j.pax.url.commons.handler.ConnectionFactory#createConfiguration(org.ops4j.util.property.PropertyResolver)
                 */
                public Void createConfiguration( PropertyResolver propertyResolver )
                {
                    return null;
                }

                public ArtifactProvider apiProvider()
                {
                    return null;
                }

            }
        );
    }

}
