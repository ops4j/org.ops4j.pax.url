/*
 * Copyright (C) 2010 Okidokiteam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.aether.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;
import org.sonatype.aether.version.Version;

import static org.ops4j.pax.url.aether.internal.Parser.VERSION_LATEST;

/**
 * Aether based, drop in replacement for mvn protocol
 */
public class AetherBasedResolver {

    private static final Log LOG = LogFactory.getLog( AetherBasedResolver.class );
    private static final String LATEST_VERSION_RANGE = "(0.0,]";
    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    private static final String FILE_DUMMY_REPO = "file:///dummy";
    private static final String REPO_TYPE = "default";

    final private RepositorySystem m_repoSystem;
    final private List<RemoteRepository> m_remoteRepos;
    final private MavenConfiguration m_config;

    /**
     * Create a AetherBasedResolver
     *
     * @param configuration (can be null)
     * @param repos         (must be not null)
     */
    public AetherBasedResolver( MavenConfiguration configuration, List<String> repos )
    {
        m_repoSystem = newRepositorySystem();
        m_config = configuration;

        m_remoteRepos = new ArrayList<RemoteRepository>();
        int i = 0;
        // aether does not really like no remote repo at all ..

        for( String r : repos ) {
            String id = "repo" + ( i++ );

            if( isAvailable( r ) ) {
                m_remoteRepos.add( new RemoteRepository( id, REPO_TYPE, r ) );
            }
            else {
                // to enable cached loading
                m_remoteRepos.add( new RemoteRepository( id, REPO_TYPE, FILE_DUMMY_REPO ) );
            }
        }

    }

    /**
     * This is a workaround for Aether 1.11 failing if at least one remote repo is not available currently.
     * Which is kind of bad.
     *
     * @param url to test for connection
     *
     * @return true if its available. Otherwise false.
     */
    private boolean isAvailable( String url )
    {
        try {
            new URL( url ).openStream().close();
            return true;
        } catch( IOException e ) {
            // e.printStackTrace();
        }
        return false;
    }

    public InputStream resolve( String groupId, String artifactId, String extension, String version )
        throws IOException
    {
        // version = mapLatestToRange( version );

        RepositorySystemSession session = newSession( m_repoSystem );
        Artifact artifact = new DefaultArtifact( groupId, artifactId, extension, version );
        File resolved = resolve( session, artifact );

        LOG.info( "Resolved (" + artifact.toString() + ") as " + resolved.getAbsolutePath() );
        return new FileInputStream( resolved );

    }

    private File resolve( RepositorySystemSession session, Artifact artifact )
        throws IOException
    {
        try {

            artifact = resolveLatestVersionRange( session, artifact );
            //  Metadata metadata = new DefaultMetadata( artifact.getGroupId(), artifact.getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT );
            //  List<MetadataResult> metadataResults = m_repoSystem.resolveMetadata( session, Arrays.asList( new MetadataRequest( metadata ) ) );

            return m_repoSystem.resolveArtifact( session, new ArtifactRequest( artifact, m_remoteRepos, null ) ).getArtifact().getFile();
        } catch( RepositoryException e ) {
            throw new IOException( "Aether Error.", e );
        }
    }

    /**
     * Tries to resolve versions = LATEST using an open range version query.
     * If it succeeds, version of artifact is set to the highest available version.
     *
     * @param session  to be used.
     * @param artifact to be used
     *
     * @return an artifact with version set properly (highest if available)
     *
     * @throws org.sonatype.aether.resolution.VersionRangeResolutionException
     *          in case of resolver errors.
     */
    private Artifact resolveLatestVersionRange( RepositorySystemSession session, Artifact artifact )
        throws VersionRangeResolutionException
    {
        if( artifact.getVersion().equals( VERSION_LATEST ) ) {
            artifact = artifact.setVersion( LATEST_VERSION_RANGE );

            VersionRangeResult versionResult = m_repoSystem.resolveVersionRange( session, new VersionRangeRequest( artifact, m_remoteRepos, null ) );
            if( versionResult != null ) {
                Version v = versionResult.getHighestVersion();
                if( v != null ) {

                    artifact = artifact.setVersion( v.toString() );
                }
                else {
                    throw new VersionRangeResolutionException( versionResult, "Not highest version found for " + artifact );
                }
            }
        }
        return artifact;
    }

    private RepositorySystemSession newSession( RepositorySystem system )
    {
        assert m_config != null : "local repository cannot be null";
        File local = m_config.getLocalRepository().getFile();
        assert local.exists() : "local repository must exist (" + local + ").";

        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        //session.setOffline( true );

        LocalRepository localRepo = new LocalRepository( local );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );

        // configure mirror
        DefaultMirrorSelector mirrorSelector = (DefaultMirrorSelector) session.getMirrorSelector();
        Map<String, Map<String, String>> mirrors = m_config.getMirrors();
        int i = 1;
        for( Map<String, String> mirror : mirrors.values() ) {
            //The fields are id, url, mirrorOf, layout, mirrorOfLayouts.
            String mirrorOf = mirror.get( "mirrorOf" );
            String url = mirror.get( "url" );
            // type can be null in this implementation (1.11)
            mirrorSelector.add( "mirrorId_" + i, url, null, false, mirrorOf, "*" );
            i++;
        }

        //configure proxies
        DefaultProxySelector proxySelector = (DefaultProxySelector) session.getProxySelector();
        Map<String, Map<String, String>> proxies = m_config.getProxySettings();
        for( Map<String, String> proxy : proxies.values() ) {
            //The fields are user, pass, host, port, nonProxyHosts, protocol.
            String nonProxyHosts = proxy.get( "nonProxyHosts" );
            Proxy proxyObj = new Proxy( proxy.get( "protocol" ),
                                        proxy.get( "host" ),
                                        toInt( proxy.get( "port" ) ),
                                        getAuthentication( proxy )
            );
            proxySelector.add( proxyObj, nonProxyHosts );
        }

        return session;
    }

    private Authentication getAuthentication( Map<String, String> proxy )
    {
        // user, pass
        if( proxy.containsKey( "user" ) ) {
            return new Authentication( proxy.get( "user" ), proxy.get( "pass" ) );
        }
        return null;
    }

    private int toInt( String intStr )
    {
        return Integer.parseInt( intStr );
    }

    private RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = new DefaultServiceLocator();

        locator.setServices( WagonProvider.class, new ManualWagonProvider() );
        locator.addService( RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class );
        locator.setService( Logger.class, LogAdapter.class );

        return locator.getService( RepositorySystem.class );
    }
}
