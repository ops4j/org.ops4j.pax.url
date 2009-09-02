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

/**
 * Parser for "assembly:" protocol wher ethe url is a path to a directory, optionally followed by an file pattern.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class UrlParser
    implements Parser
{

    private final Source[] m_sources;

    /**
     * Constructor.
     *
     * @param url the path part of the url (without starting assembly:)
     *
     * @throws MalformedURLException - If provided url does not comply to expected syntax
     */
    UrlParser( final String url )
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
        m_sources = new Source[]{ new PathEncodedSource( url ) };
    }

    /**
     * Always returns null as "assembly" protocol does not support manifest.
     *
     * {@inheritDoc}
     */
    public URL manifest()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Source[] sources()
    {
        return m_sources;
    }

    /**
     * Always returns first wins merge policy.
     *
     * {@inheritDoc}
     */
    public MergePolicy mergePolicy()
    {
        return MergePolicy.FIRST;
    }

}