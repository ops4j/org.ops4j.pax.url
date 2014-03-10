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
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipFile;

import org.ops4j.io.DirectoryLister;
import org.ops4j.io.HierarchicalIOException;
import org.ops4j.io.Lister;
import org.ops4j.io.ZipLister;
import org.ops4j.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAVADOC
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class ResourceAssembly
    implements Iterable<Resource>
{

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger( ResourceAssembly.class );

    /**
     * Resources that makes up this assembly.
     */
    private final Set<Resource> m_resources;

    /**
     * Constructor.
     *
     * @param sources sources that makes up this assembly
     * @param policy  merging policy
     *
     * @throws IOException - If a problem encountered during resource scanning process
     */
    public ResourceAssembly( final Source[] sources,
                             final MergePolicy policy )
        throws IOException
    {
        NullArgumentException.validateNotNull( sources, "Resource patterns" );
        NullArgumentException.validateNotNull( policy, "Merging policy" );

        m_resources = new HashSet<Resource>();
        for( Source source : sources )
        {
            scan( source, policy );
        }
    }

    private void scan( final Source source,
                       final MergePolicy policy )
        throws IOException
    {
        // try out an url
        LOGGER.trace( "Searching for [" + source + "]" );
        final String path = source.path();
        URL url = null;
        try
        {
            url = new URL( path );
        }
        catch( MalformedURLException ignore )
        {
            // ignore this as the spec may be resolved other way
            LOGGER.trace(
                String.format(
                    "Path [%s] is not a valid url. Reason: %s. Continue discovery...", path, ignore.getMessage()
                )
            );
        }
        File file = null;
        if( url != null && "file".equals( url.getProtocol() ) )
        // if we have an url and it's a file url
        {
            try
            {
                final URI uri = new URI( url.toExternalForm().replaceAll( " ", "%20" ) );
                file = new File( uri );
            }
            catch( URISyntaxException ignore )
            {
                // ignore this as the spec may be resolved other way
                LOGGER.trace(
                    String.format(
                        "Path [%s] is not a valid url. Reason: %s. Continue discovery...", path, ignore.getMessage()
                    )
                );
            }
        }
        else
        // if we don't have an url then let's try out a direct file
        {
            file = new File( path );
        }
        if( file != null && file.exists() )
        // if we have a directory
        {
            if( file.isDirectory() )
            {
                list( file, new DirectoryLister( file, source.includes(), source.excludes() ), policy );
                return;
            }
            else
            {
                LOGGER.trace( String.format( "Path [%s] is not a valid directory. Continue discovery...", path ) );
            }
        }
        else
        {
            LOGGER.trace( String.format( "Path [%s] is not a valid file. Continue discovery...", path ) );
        }
        // on this point we may have a zip
        try
        {
            ZipFile zip = null;
            URL baseUrl = null;
            if( file != null && file.exists() )
            // try out a zip from the file we have
            {
                zip = new ZipFile( file );
                baseUrl = file.toURI().toURL();
            }
            else if( url != null )
            {
                zip = new ZipFile( url.toExternalForm() );
                baseUrl = url;
            }
            if( zip != null && baseUrl != null )
            {
                list( new ZipLister( baseUrl, zip.entries(), source.includes(), source.excludes() ), policy );
                return;
            }
        }
        catch( IOException ignore )
        {
            // ignore for the moment
            LOGGER.trace(
                String.format(
                    "Path [%s] is not a valid zip. Reason: %s. Continue discovery...", path, ignore.getMessage()
                )
            );
        }
        // finally try with a zip protocol
        if( url != null && !url.toExternalForm().startsWith( "jar" ) )
        {
            try
            {
                final URL jarUrl = new URL( "jar:" + url.toURI().toASCIIString() + "!/" );
                final JarURLConnection jar = (JarURLConnection) jarUrl.openConnection();
                list( new ZipLister( url, jar.getJarFile().entries(), source.includes(), source.excludes() ), policy );
                return;
            }
            catch( IOException ignore )
            {
                LOGGER.trace( String.format( "Path [%s] is not a valid jar. Reason: %s", path, ignore.getMessage() ) );
            }
            catch( URISyntaxException ignore )
            {
                LOGGER.trace( String.format( "Path [%s] is not a valid jar. Reason: %s", path, ignore.getMessage() ) );
            }
        }
        // if we got to this point then we cannot go further
        LOGGER.trace( String.format( "Source [%s] cannot be used. Stopping.", source ) );
        throw new HierarchicalIOException( String.format( "Source [%s] cannot be used. Stopping.", source ) );
    }

    private void list( final File base,
                       final Lister lister,
                       final MergePolicy policy )
        throws IOException
    {
        for( URL url : lister.list() )
        {
            try
            {
                policy.addResource( new FileResource( base, new File( url.toURI() ) ), m_resources );
            }
            catch( URISyntaxException e )
            {
                throw new HierarchicalIOException(
                    String.format( "URL [%s] could not be used", url.toExternalForm() )
                );
            }
        }
    }

    private void list( final Lister lister,
                       final MergePolicy policy )
        throws IOException
    {
        for( URL url : lister.list() )
        {
            policy.addResource( new JarResource( url ), m_resources );
        }
    }

    /**
     * Returns an iterator over assembly resources.
     *
     * {@inheritDoc}
     */
    public Iterator<Resource> iterator()
    {
        return m_resources.iterator();
    }
}