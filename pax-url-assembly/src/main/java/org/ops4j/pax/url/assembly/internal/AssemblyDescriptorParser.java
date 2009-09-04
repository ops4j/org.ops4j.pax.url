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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.ops4j.io.ListerUtils;

/**
 * Parser for "assemblyref:" protocol where the url referes to an assembly descriptor file.
 * Descriptor file should be a json file.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class AssemblyDescriptorParser
    implements Parser
{

    /**
     * Parsed manifest path.
     */
    private String m_manifest;
    /**
     * Parsed array of sources.
     */
    private Source[] m_sources;
    /**
     * Parsed merge policy.
     */
    private MergePolicy m_mergePolicy;

    /**
     * Constructor.
     *
     * @param url the path part of the url (without starting assemblyref:)
     *
     * @throws IOException - IIf a problem encountered while parsing descriptor file
     */
    AssemblyDescriptorParser( final String url )
        throws IOException
    {
        if( url == null )
        {
            throw new MalformedURLException( "Url cannot be null. Syntax " + SYNTAX );
        }
        if( "".equals( url.trim() ) || "/".equals( url.trim() ) )
        {
            throw new MalformedURLException( "Url cannot be empty. Syntax " + SYNTAX );
        }

        m_sources = new Source[0];
        m_mergePolicy = MergePolicy.FIRST;

        parseDescriptor( new URL( url ) );
    }

    /**
     * {@inheritDoc}
     */
    public String manifest()
    {
        return m_manifest;
    }

    /**
     * {@inheritDoc}
     */
    public Source[] sources()
    {
        return m_sources;
    }

    /**
     * Returns merge policy based on descriptor. If not specified returns a first wins merge policy.
     *
     * {@inheritDoc}
     */
    public MergePolicy mergePolicy()
    {
        return m_mergePolicy;
    }

    /**
     * Reads descriptor file.
     *
     * @param url descriptor file url
     *
     * @throws IOException - If a problem encountered while parsing descriptor file
     */
    private void parseDescriptor( final URL url )
        throws IOException
    {
        final JsonFactory jFactory = new JsonFactory();
        final JsonParser jp = jFactory.createJsonParser( url );
        if( jp.nextToken() != JsonToken.START_OBJECT )
        {
            throw new IOException( String.format( "Descriptor [%s] not in JSON format", url.toExternalForm() ) );
        }
        final Collection<Source> sources = new HashSet<Source>();
        try
        {
            while( jp.nextToken() != JsonToken.END_OBJECT )
            {
                final String currentName = jp.getCurrentName();
                jp.nextToken();

                if( "manifest".equals( currentName ) )
                {
                    m_manifest = jp.getText();
                }
                else if( "assembly".equals( currentName ) )
                {
                    sources.addAll( parseAssembly( jp ) );
                }
                else if( "mergePolicy".equals( currentName ) )
                {
                    m_mergePolicy = "last".equalsIgnoreCase( jp.getText() ) ? MergePolicy.LAST : MergePolicy.FIRST;
                }
            }
        }
        finally
        {
            jp.close();
        }
        m_sources = sources.toArray( new Source[sources.size()] );
    }

    /**
     * Parses "directories" section.
     *
     * @param jp json parser.
     *
     * @return parsed sources
     *
     * @throws IOException - If a problem encountered while parsing descriptor file
     */
    private Collection<Source> parseAssembly( final JsonParser jp )
        throws IOException
    {
        final Collection<Source> sources = new HashSet<Source>();

        while( jp.nextToken() != JsonToken.END_OBJECT )
        {
            final String currentName = jp.getCurrentName();
            jp.nextToken();

            if( "directory".equals( currentName )
                || "jar".equals( currentName )
                || "zip".equals( currentName) )
            {
                final Source source = parseDirectory( jp );
                sources.add( source );
            }
        }
        return sources;
    }

    /**
     * Parses "directory" section.
     *
     * @param jp json parser.
     *
     * @return parsed source
     *
     * @throws IOException - If a problem encountered while parsing descriptor file
     */
    private Source parseDirectory( final JsonParser jp )
        throws IOException
    {
        String path = null;
        final Collection<Pattern> includes = new HashSet<Pattern>();
        final Collection<Pattern> excludes = new HashSet<Pattern>();

        while( jp.nextToken() != JsonToken.END_OBJECT )
        {
            final String currentName = jp.getCurrentName();
            jp.nextToken();

            if( "path".equals( currentName ) )
            {
                path = jp.getText();
            }
            else if( "include".equals( currentName ) )
            {
                includes.add( ListerUtils.parseFilter( jp.getText() ) );
            }
            else if( "exclude".equals( currentName ) )
            {
                excludes.add( ListerUtils.parseFilter( jp.getText() ) );
            }
        }
        if( path == null )
        {
            throw new IOException( "Invalid descriptor file" );
        }
        return new ImmutableSource(
            path, includes.toArray( new Pattern[includes.size()] ), excludes.toArray( new Pattern[excludes.size()] )
        );
    }

}