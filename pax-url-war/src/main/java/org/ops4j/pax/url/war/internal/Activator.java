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
package org.ops4j.pax.url.war.internal;

import java.net.URL;
import java.net.URLConnection;
import org.ops4j.pax.runner.commons.resolver.Resolver;
import org.ops4j.pax.url.commons.ConnectionFactory;
import org.ops4j.pax.url.commons.HandlerActivator;
import org.ops4j.pax.url.war.ServiceConstants;

/**
 * Bundle activator for war: protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 13, 2007
 */
public final class Activator
    extends HandlerActivator
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory()
            {

                public URLConnection createConection( final URL url, final Resolver resolver )
                {
                    // TODO implemnt connection creation
                    return null;
                }

            }
        );
    }
}