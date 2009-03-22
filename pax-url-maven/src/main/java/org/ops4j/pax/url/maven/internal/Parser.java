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
package org.ops4j.pax.url.maven.internal;

import java.net.MalformedURLException;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;

/**
 * Parser for "maven:" protocol.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.0, March 22, 2009
 */
public class Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "maven:[repository_url!]groupId:artifactId[:[version][:[classifier][:type]]]";

    /**
     * Separator between repository and artifact definition.
     */
    private static final String REPOSITORY_SEPARATOR = "!";
    /**
     * Repository URL. Null if not present.
     */
    private MavenRepositoryURL m_repositoryURL;
    /**
     * GAV.
     */
    private String m_gav;

    /**
     * Creates a new protocol parser.
     *
     * @param path the path part of the url (without starting maven:)
     *
     * @throws java.net.MalformedURLException - if provided path does not comply to expected syntax or an malformed
     *                                        repository URL
     */
    public Parser( final String path )
        throws MalformedURLException
    {
        if( path == null )
        {
            throw new MalformedURLException( "Path cannot be null. Syntax " + SYNTAX );
        }
        if( path.startsWith( REPOSITORY_SEPARATOR ) || path.endsWith( REPOSITORY_SEPARATOR ) )
        {
            throw new MalformedURLException(
                "Path cannot start or end with " + REPOSITORY_SEPARATOR + ". Syntax " + SYNTAX
            );
        }
        if( path.contains( REPOSITORY_SEPARATOR ) )
        {
            int pos = path.lastIndexOf( REPOSITORY_SEPARATOR );
            m_gav = path.substring( pos + 1 );
            m_repositoryURL = new MavenRepositoryURL( path.substring( 0, pos ) + "@snapshots" );
        }
        else
        {
            m_gav = path;
        }
        if( m_gav == null || m_gav.trim().length() == 0 )
        {
            throw new MalformedURLException( "GAV cannot be null. Syntax " + SYNTAX );
        }
    }

    /**
     * Returns the repository URL if present, null otherwise
     *
     * @return repository URL
     */
    public MavenRepositoryURL getRepositoryURL()
    {
        return m_repositoryURL;
    }

    /**
     * Returns the GAV of the artifact.
     *
     * @return GAV
     */
    public String getGAV()
    {
        return m_gav;
    }

}