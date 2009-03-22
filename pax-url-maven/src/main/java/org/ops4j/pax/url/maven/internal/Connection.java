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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.Quality;
import org.apache.maven.mercury.artifact.QualityRange;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.ArtifactBasicResults;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.transport.api.Server;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;

/**
 * An URLConnextion that supports maven: protocol.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.0, March 22, 2009
 */
public class Connection
    extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Connection.class );
    /**
     * Service configuration.
     */
    private final MavenConfiguration m_configuration;
    /**
     * Parsed url.
     */
    private Parser m_parser;
    /**
     * Mercury virtual repository reader.
     */
    private VirtualRepositoryReader m_vrr;

    /**
     * Creates a new connection.
     *
     * @param url           the url; cannot be null.
     * @param configuration service configuration; cannot be null
     *
     * @throws java.net.MalformedURLException in case of a malformed url
     */
    public Connection( final URL url, final MavenConfiguration configuration )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( configuration, "Service configuration" );
        m_configuration = configuration;
        m_parser = new Parser( url.getPath() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect()
        throws IOException
    {
        final List<Repository> repositories = new ArrayList<Repository>();
        final MavenRepositoryURL localRepository = m_configuration.getLocalRepository();
        if( localRepository != null )
        {
            LOG.debug( "Using local repository " + localRepository );
            repositories.add( toRepository( localRepository ) );
        }
        final List<MavenRepositoryURL> remoteRepos = new ArrayList<MavenRepositoryURL>();
        if( m_parser.getRepositoryURL() != null )
        {
            remoteRepos.add( m_parser.getRepositoryURL() );
        }
        remoteRepos.addAll( m_configuration.getRepositories() );
        for( MavenRepositoryURL repositoryURL : remoteRepos )
        {
            LOG.debug( "Using remote repository " + repositoryURL );
            repositories.add( toRepository( repositoryURL ) );
        }
        try
        {
            m_vrr = new VirtualRepositoryReader( repositories );
        }
        catch( RepositoryException e )
        {
            throw initIOException( "Cannot configure Maven repositories", e );
        }
    }

    /**
     * Adapt a {@link MavenRepositoryURL} to a Mercury {@link Repository}.
     *
     * @param repositoryURL to adapt
     *
     * @return adapted
     */
    private Repository toRepository( final MavenRepositoryURL repositoryURL )
    {
        final Repository repository;
        if( repositoryURL.isFileRepository() )
        {
            repository = new LocalRepositoryM2(
                repositoryURL.getId(), repositoryURL.getFile(), DependencyProcessor.NULL_PROCESSOR
            );
        }
        else
        {
            final Server server = new Server( repositoryURL.getId(), repositoryURL.getURL() );
            repository = new RemoteRepositoryM2( server.getId(), server, DependencyProcessor.NULL_PROCESSOR );
        }
        repository.setRepositoryQualityRange(
            createQualityRange( repositoryURL.isReleasesEnabled(), repositoryURL.isSnapshotsEnabled() )
        );
        return repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();

        LOG.debug( "Resolving [" + url.toExternalForm() + "]" );

        List<ArtifactBasicMetadata> query = new ArrayList<ArtifactBasicMetadata>();
        final ArtifactMetadata queryMeta = new ArtifactMetadata( m_parser.getGAV() );
        query.add( queryMeta );

        final ArtifactBasicResults results;
        try
        {
            results = m_vrr.readVersions( query );
        }
        catch( RepositoryException e )
        {
            throw initIOException( "Canot determine artifacts versions", e );
        }
        if( results == null )
        {
            throw new IOException( "Cannot determine artifact versions from [" + m_parser.getGAV() );
        }
        if( results.hasExceptions() || !results.hasResults( queryMeta ) )
        {
            //noinspection ThrowableResultOfMethodCallIgnored
            throw initIOException( "Cannot determine artifact versions", results.getError( queryMeta ) );
        }

        final List<ArtifactBasicMetadata> foundArtifacts = results.getResult( queryMeta );
        if( LOG.isDebugEnabled() )
        {
            for( ArtifactBasicMetadata foundArtifact : foundArtifacts )
            {
                LOG.debug( "Found artifact [" + foundArtifact.getGAV() + "]" );
            }

            // download the artifact
            // TODO selecting the last artifact in the list is not the correct approach
            final ArtifactBasicMetadata selectedArtifact = foundArtifacts.get( foundArtifacts.size() - 1 );

            LOG.debug( "Selected artifact [" + selectedArtifact.getGAV() + "]" );

            final ArtifactResults artifactResults;
            try
            {
                artifactResults = m_vrr.readArtifacts( toList( selectedArtifact ) );
            }
            catch( RepositoryException e )
            {
                e.printStackTrace();
                throw initIOException( "Canot download artifact [" + selectedArtifact.getGAV() + "]", e );
            }
            if( artifactResults.hasExceptions() || !artifactResults.hasResults( selectedArtifact ) )
            {
                //noinspection ThrowableResultOfMethodCallIgnored
                throw initIOException(
                    "Cannot download artifact [" + selectedArtifact.getGAV() + "]",
                    artifactResults.getError( selectedArtifact )
                );
            }
            final List<Artifact> artifacts = artifactResults.getResults( selectedArtifact );
            final File artifactFile = artifacts.get( 0 ).getFile();
            if( artifactFile == null )
            {
                throw new IOException( "Cannot download artifact [" + selectedArtifact.getGAV() );
            }
            return new BufferedInputStream( new FileInputStream( artifactFile ) );
        }

        // no artifact found
        throw new RuntimeException(
            "URL [" + url.toExternalForm() + "] could not be resolved. (enable TRACE logging for details)"
        );
    }

    //TODO remove once a version > 1.0-alpha-5 of Mercury and use QualityRange.create
    private static QualityRange createQualityRange( boolean releases, boolean snapshots )
    {
        if( releases && snapshots )
        {
            return QualityRange.ALL;
        }
        else if( releases )
        {
            return new QualityRange( Quality.ALPHA_QUALITY, true, Quality.RELEASE_QUALITY, true );
        }
        else if( snapshots )
        {
            return new QualityRange( Quality.SNAPSHOT_QUALITY, true, Quality.SNAPSHOT_TS_QUALITY, true );
        }

        throw new IllegalArgumentException( "Unsuported combination for releases/snapshots" );
    }

    /**
     * Creates an IOException with a message and a cause.
     *
     * @param message exception message
     * @param cause   exception cause
     *
     * @return the created IO Exception
     */
    private IOException initIOException( final String message, final Exception cause )
    {
        IOException exception = new IOException( message );
        exception.initCause( cause );
        return exception;
    }

    /**
     * Utility to add var args elements into a list.
     *
     * @param elements to add
     * @param <T>      any type
     *
     * @return list
     */
    private <T> List<T> toList( final T... elements )
    {
        final ArrayList<T> list = new ArrayList<T>();
        list.addAll( Arrays.asList( elements ) );
        return list;
    }

}