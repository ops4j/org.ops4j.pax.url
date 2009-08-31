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
import java.util.HashSet;
import java.util.Set;
import org.ops4j.lang.NullArgumentException;

/**
 * JAVADOC
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class DirectoryAssembly
{

    /**
     * Set of patterns of resources that make up this assembly.
     */
    private final Set<String> m_patterns;
    /**
     * Merging policy of resources.
     */
    private final MergePolicy m_policy;

    public DirectoryAssembly( final Set<String> patterns,
                              final MergePolicy policy )
    {
        NullArgumentException.validateNotNull( patterns, "Resource patterns" );
        NullArgumentException.validateNotNull( policy, "Merging policy" );

        m_patterns = patterns;
        m_policy = policy;
    }

    Set<Resource> scanResources()
        throws IOException
    {
        final Set<Resource> resources = new HashSet<Resource>();
        for( String pattern : m_patterns )
        {
            if( pattern.startsWith( "jar:" ) )
            {
                // TODO implement jar scanning
            }
            else
            {
                // TODO implement file scanning
            }
        }
        return resources;
    }

}