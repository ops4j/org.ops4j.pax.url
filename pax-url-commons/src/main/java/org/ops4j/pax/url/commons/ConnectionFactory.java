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
package org.ops4j.pax.url.commons;

import java.net.URL;
import java.net.URLConnection;
import org.ops4j.pax.runner.commons.resolver.Resolver;

/**
 * URL connection factory.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 13, 2008
 */
public interface ConnectionFactory
{

    /**
     * Creates a handler specific conection.
     *
     * @param url      url to be handled
     * @param resolver configuration resolver
     *
     * @return URLConnection
     */
    URLConnection createConection( URL url, Resolver resolver );

}
