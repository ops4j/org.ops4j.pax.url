/*
 * Copyright (C) 2010 Toni Menzel
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
package org.ops4j.pax.url.mvn.internal;

import static org.ops4j.pax.url.mvn.Parser.VERSION_LATEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManagerFactory;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.ProxySelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;
import org.sonatype.aether.version.Version;

/**
 * Aether based, drop in replacement for mvn protocol
 */
public class AetherBasedResolver {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger( AetherBasedResolver.class );
    private static final String LATEST_VERSION_RANGE = "(0.0,]";
    private static final String REPO_TYPE = "default";

    final private RepositorySystem m_repoSystem;
    final private List<RemoteRepository> m_remoteRepos;
    final private MavenConfiguration m_config;
    final private MirrorSelector m_mirrorSelector;
    final private ProxySelector m_proxySelector;

    /**
     * Create a AetherBasedResolver
     *
     * @param configuration (must be not null)
     *
     * @throws java.net.MalformedURLException in case of url problems in configuration.
     */
    public AetherBasedResolver( final MavenConfiguration configuration )
        throws MalformedURLException
    {
        m_repoSystem = newRepositorySystem();
        m_config = configuration;

        m_remoteRepos = selectRepositories( getRemoteRepositories( configuration ) );
        m_mirrorSelector = selectMirrors();
        m_proxySelector = selectProxies();
        assignProxyAndMirrors();
    }

    private void assignProxyAndMirrors()
    {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        Map<String, RemoteRepository> naming = new HashMap<String, RemoteRepository>();

        for( RemoteRepository r : m_remoteRepos ) {
            naming.put( r.getId(), r );

            r.setProxy( m_proxySelector.getProxy( r ) );

            RemoteRepository mirror = m_mirrorSelector.getMirror( r );
            if( mirror != null ) {
                String key = mirror.getId();
                naming.put( key, mirror );
                if( !map.containsKey( key ) ) {
                    map.put( key, new ArrayList<String>() );
                }
                List<String> mirrored = map.get( key );
                mirrored.add( r.getId() );
            }
        }

        for( String mirrorId : map.keySet() ) {
            RemoteRepository mirror = naming.get( mirrorId );
            List<RemoteRepository> mirroedRepos = new ArrayList<RemoteRepository>();

            for( String rep : map.get( mirrorId ) ) {
                mirroedRepos.add( naming.get( rep ) );
            }
            mirror.setMirroredRepositories( mirroedRepos );
            m_remoteRepos.removeAll(mirroedRepos);
            m_remoteRepos.add( 0, mirror );
        }

    }

    private List<MavenRepositoryURL> getRemoteRepositories( MavenConfiguration configuration )
        throws MalformedURLException
    {
        List<MavenRepositoryURL> r = new ArrayList<MavenRepositoryURL>();
        for( MavenRepositoryURL s : configuration.getRepositories() ) {
            r.add( s );
        }
        return r;
    }

    private ProxySelector selectProxies()
    {
        DefaultProxySelector proxySelector = new DefaultProxySelector();
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
        return proxySelector;
    }

    private MirrorSelector selectMirrors()
    {
        // configure mirror
        DefaultMirrorSelector selector = new DefaultMirrorSelector();
        Map<String, Map<String, String>> mirrors = m_config.getMirrors();

        for( String mirrorName : mirrors.keySet() ) {
            Map<String, String> mirror = mirrors.get( mirrorName );
            //The fields are id, url, mirrorOf, layout, mirrorOfLayouts.
            String mirrorOf = mirror.get( "mirrorOf" );
            String url = mirror.get( "url" );
            // type can be null in this implementation (1.11)
            selector.add( mirrorName, url, null, false, mirrorOf, "*" );
        }
        return selector;
        /**
         Set<RemoteRepository> mirrorRepoList = new HashSet<RemoteRepository>();
         for (RemoteRepository r : m_remoteRepos) {
         RemoteRepository mirrorRepo = mirrorSelector.getMirror(r);
         if (mirrorRepo != null)
         {
         mirrorRepoList.add(mirrorRepo);
         }
         }
         return mirrorRepoList;
         **/
    }

    private List<RemoteRepository> selectRepositories( List<MavenRepositoryURL> repos )
    {
        List<RemoteRepository> list = new ArrayList<RemoteRepository>();
        for( MavenRepositoryURL r : repos ) {
            list.add( new RemoteRepository( r.getId(), REPO_TYPE, r.getURL().toExternalForm() ) );
        }
        return list;
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

    public InputStream resolve( String groupId, String artifactId, String classifier, String extension, String version )
        throws IOException
    {
        // version = mapLatestToRange( version );
        RepositorySystemSession session = newSession();

        Artifact artifact = new DefaultArtifact( groupId, artifactId, classifier, extension, version );
        File resolved = resolve( session, artifact );

        LOG.debug( "Resolved ({}) as {}", artifact.toString(), resolved.getAbsolutePath() );
        return new FileInputStream( resolved );
    }

    private File resolve( RepositorySystemSession session, Artifact artifact )
        throws IOException
    {
        try {
            artifact = resolveLatestVersionRange( session, artifact );
            return m_repoSystem.resolveArtifact( session, new ArtifactRequest( artifact, m_remoteRepos, null ) ).getArtifact().getFile();
        } catch( RepositoryException e ) {
            throw new IOException( "Error resolving artifact " + artifact.toString(), e );
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

    private RepositorySystemSession newSession()
    {
        assert m_config != null : "local repository cannot be null";
        File local = m_config.getLocalRepository().getFile();

        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository( local );

        session.setLocalRepositoryManager( m_repoSystem.newLocalRepositoryManager( localRepo ) );
        session.setMirrorSelector( m_mirrorSelector );
        session.setProxySelector( m_proxySelector );
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

        locator.setService( LocalRepositoryManagerFactory.class, SimpleLocalRepositoryManagerFactory.class );
        locator.setService( Logger.class, LogAdapter.class );

        return locator.getService( RepositorySystem.class );
    }
}
