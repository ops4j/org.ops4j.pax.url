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

import java.io.File;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.net.URL;

/**
 * Resource is an url that has a path.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
public interface Resource
{

    /**
     * Getter.
     * @return resource path
     */
    String path();

    /**
     * Getter.
     *
     * @return resource url
     */
    URL url();

}