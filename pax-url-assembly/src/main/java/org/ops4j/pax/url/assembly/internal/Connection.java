/*
 * Copyright 2009 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.assembly.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import org.ops4j.lang.NullArgumentException;

/**
 * JAVADOC.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 1.1.0, August 31, 2009
 */
class Connection
    extends URLConnection
{

    /**
     * Parsed url.
     */
    private Parser m_parser;

    /**
     * Creates a new connection.
     *
     * @param url    url to be handled
     * @param parser url parser
     *
     * @throws MalformedURLException - If url path is empty
     * @throws IOException           - If cache name cannot be generated
     * @throws NullArgumentException - If url or parser is null
     */
    Connection( final URL url,
                final Parser parser )
        throws IOException
    {
        super( url );

        NullArgumentException.validateNotNull( url, "URL" );
        NullArgumentException.validateNotNull( parser, "Parser" );

        m_parser = parser;
    }

    /**
     * Does nothing.
     *
     * {@inheritDoc}
     */
    @Override
    public void connect()
    {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        final Set<Resource> resources = new DirectoryAssembly( m_parser.patterns() ).scanResources();
        final URL manifest = getManifest( resources );
        final VirtualJar virtualJar = new VirtualJar( manifest, resources );
        return virtualJar.inputStream();
    }

    /**
     * Determines the manifest to be used. If parser specifies an manifest it uses that manifest, otherwise it takes
     * first resource with a path of "META-INF/MANIFEST.MF".
     *
     * @param resources set of resources that may contain an manifest
     *
     * @return manifest url or null if none
     */
    private URL getManifest( Set<Resource> resources )
    {
        if( m_parser.manifest() != null )
        {
            return m_parser.manifest();
        }
        if( resources != null )
        {
            for( Resource resource : resources )
            {
                if( "META-INF/MANIFEST.MF".equals( resource.path() ) )
                {
                    return resource.url();
                }
            }
        }
        return null;
    }
}