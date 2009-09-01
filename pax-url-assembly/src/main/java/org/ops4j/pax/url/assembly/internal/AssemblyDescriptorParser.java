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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Parser for "assemblyref:" protocol where the url referes to an assembly descriptor file.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class AssemblyDescriptorParser
    implements Parser
{

    /**
     * Url of descriptor file.
     */
    private final URL m_descriptorUrl;

    /**
     * Constructor.
     *
     * @param url the path part of the url (without starting assemblyref:)
     *
     * @throws java.net.MalformedURLException - If provided url does not comply to expected syntax
     */
    AssemblyDescriptorParser( final String url )
        throws MalformedURLException
    {
        if( url == null )
        {
            throw new MalformedURLException( "Url cannot be null. Syntax " + SYNTAX );
        }
        if( "".equals( url.trim() ) || "/".equals( url.trim() ) )
        {
            throw new MalformedURLException( "Url cannot be empty. Syntax " + SYNTAX );
        }
        m_descriptorUrl = new URL( url );
    }

    /**
     * Returns the parsed manifest url.
     *
     * @return parsed manifest url
     */
    public URL manifest()
    {
        // TODO implement method
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the parsed resource patterns to be included into assembly. It should not return null if there is no
     * pattern but an empty set.
     *
     * @return parsed resource patterns
     */
    public Set<String> patterns()
    {
        // TODO implement method
        throw new UnsupportedOperationException();
    }

    /**
     * Returns merge policy based on descriptor. If not specified returns a first wins merge policy.
     *
     * {@inheritDoc}
     */
    public MergePolicy mergePolicy()
    {
        return MergePolicy.FIRST;
        // TODO read merge policy from descriptor
    }

}