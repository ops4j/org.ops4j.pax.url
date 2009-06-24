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
package org.ops4j.pax.url.manifest.internal;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Parser for "manifest:" protocol.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, June 24, 2009
 */
public class Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "manifest:Manifest-URL,Jar-URL";
    /**
     * Parsed URL.
     */
    private URL m_url;

    /**
     * Creates a new protocol parser.
     *
     * @param url the path part of the url (without starting manifest:)
     *
     * @throws MalformedURLException - If provided path does not comply to expected syntax
     */
    public Parser( final String url )
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
        m_url = new URL( url );
    }

    /**
     * Return the parsed url.
     *
     * @return parsed url
     */
    public URL getUrl()
    {
        return m_url;
    }

}