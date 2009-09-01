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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.ops4j.io.DirectoryLister;
import org.ops4j.io.Lister;
import org.ops4j.io.ListerUtils;
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
            String base = pattern;
            String filter = "**";
            if( pattern.contains( "!/" ) )
            {
                final int startOfFilter = pattern.lastIndexOf( "!/" );
                base = pattern.substring( 0, startOfFilter );
                filter = pattern.substring( startOfFilter + 2 );
            }
            if( base.startsWith( "jar:" ) || base.startsWith( "zip:" ) )
            {
                // TODO implement jar scanning
            }
            else
            {
                scanDirectory( resources, base, filter );
            }
        }
        return resources;
    }

    private void scanDirectory( Set<Resource> resources,
                                String base,
                                String filter )
        throws IOException
    {
        File baseDir;
        try
        {
            baseDir = new File( new URI( base ) );
        }
        catch( Exception e )
        {
            baseDir = new File( base );
        }
        if( !baseDir.exists() )
        {
            throw new IOException(
                String.format( "Pattern [%s] could not be used as it does not refer to a file", base )
            );
        }
        Lister lister = new DirectoryLister( baseDir, ListerUtils.parseFilter( filter ) );
        for( URL url : lister.list() )
        {
            try
            {
                m_policy.addResource( new FileResource( baseDir, new File( url.toURI() ) ), resources );
            }
            catch( URISyntaxException e )
            {
                IOException io = new IOException(
                    String.format( "URL [%s] could not be used", url.toExternalForm() )
                );
                io.initCause( e );
                throw io;
            }
        }
    }

}