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

import java.net.URL;
import java.util.Set;

/**
 * Parser for "assembly:" protocol.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
public interface Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    static final String SYNTAX = "assembly:dir|assembly-file-url";

    /**
     * Returns the parsed manifest url.
     *
     * @return parsed manifest url
     */
    public URL manifest();

    /**
     * Returns the parsed resource patterns to be included into assembly. It should not return null if there is no
     * pattern but an empty set.
     *
     * @return parsed resource patterns
     */
    public Set<String> patterns();

}