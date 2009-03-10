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
package org.ops4j.pax.url.link.internal;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Parser for "link" protocol.
 *
 * @author Alin Dreghiciu
 * @see Connection
 * @since 0.5.0, March 10, 2009
 */
public class Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "link:URL";
    /**
     * Parsed URL.
     */
    private URL m_referencedUrl;

    /**
     * Creates a new protocol parser.
     *
     * @param url the path part of the url (without starting link:)
     *
     * @throws java.net.MalformedURLException if provided path does not comply to expected syntax
     */
    public Parser( final String url )
        throws MalformedURLException
    {
        if( url == null )
        {
            throw new MalformedURLException( "Referenced url cannot be null. Syntax " + SYNTAX );
        }
        if( "".equals( url.trim() ) || "/".equals( url.trim() ) )
        {
            throw new MalformedURLException( "Referenced cannot be empty. Syntax " + SYNTAX );
        }
        m_referencedUrl = new URL( url );
    }

    /**
     * Return the parsed url.
     *
     * @return parsed url
     */
    public URL getReferencedUrl()
    {
        return m_referencedUrl;
    }

}