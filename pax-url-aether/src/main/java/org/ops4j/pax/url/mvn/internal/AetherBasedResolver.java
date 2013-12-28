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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.Slf4jLoggerFactory;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.version.Version;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

/**
 * Aether based, drop in replacement for mvn protocol
 */
public class AetherBasedResolver {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger( AetherBasedResolver.class );
    private static final String LATEST_VERSION_RANGE = "(0.0,]";
    private static final String REPO_TYPE = "default";

    final private RepositorySystem m_repoSystem;
    final private MavenConfiguration m_config;
    final private MirrorSelector m_mirrorSelector;
    final private ProxySelector m_proxySelector;
    private Settings m_settings;
    private ConfigurableSettingsDecrypter decrypter;

    /**
     * Create a AetherBasedResolver
     * 
     * @param configuration
     *            (must be not null)
     * 
     * @throws java.net.MalformedURLException
     *             in case of url problems in configuration.
     */
    public AetherBasedResolver( final MavenConfiguration configuration )
        throws MalformedURLException {
        m_config = configuration;
        m_settings = configuration.getSettings();
        m_repoSystem = newRepositorySystem();
        decryptSettings();
        m_proxySelector = selectProxies();
        m_mirrorSelector = selectMirrors();
    }

    private void decryptSettings()
    {
        SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest( m_settings );
        SettingsDecryptionResult result = decrypter.decrypt( request );
        m_settings.setProxies( result.getProxies() );
        m_settings.setServers( result.getServers() );
    }

    private void assignProxyAndMirrors( List<RemoteRepository> remoteRepos ) {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        Map<String, RemoteRepository> naming = new HashMap<String, RemoteRepository>();

        List<RemoteRepository> resultingRepos = new ArrayList<RemoteRepository>();

        for( RemoteRepository r : remoteRepos ) {
            naming.put( r.getId(), r );

            RemoteRepository rProxy = new RemoteRepository.Builder( r ).setProxy(
                m_proxySelector.getProxy( r ) ).build();
            resultingRepos.add( rProxy );

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
            mirror = new RemoteRepository.Builder( mirror ).setMirroredRepositories( mirroedRepos )
                .build();
            resultingRepos.removeAll( mirroedRepos );
            resultingRepos.add( 0, mirror );
        }

        remoteRepos.clear();
        remoteRepos.addAll( resultingRepos );
    }

    private Collection<Repository> getRemoteRepositories( MavenConfiguration configuration )
        throws MalformedURLException {
        Map<String, Repository> repoMap = new LinkedHashMap<String, Repository>();
        List<String> activeProfiles = m_settings.getActiveProfiles();
        for( String profileId : activeProfiles ) {
            for( Profile profile : m_settings.getProfiles() ) {
                if( profile.getId().equals( profileId ) ) {
                    for( Repository repo : profile.getRepositories() ) {
                        repoMap.put( repo.getId(), repo );
                    }
                }
            }
        }

        return repoMap.values();
    }

    private ProxySelector selectProxies() {
        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for( org.apache.maven.settings.Proxy proxy : m_settings.getProxies() ) {
            String nonProxyHosts = proxy.getNonProxyHosts();
            Proxy proxyObj = new Proxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(),
                getAuthentication( proxy ) );
            proxySelector.add( proxyObj, nonProxyHosts );
        }
        return proxySelector;
    }

    private MirrorSelector selectMirrors() {
        // configure mirror
        DefaultMirrorSelector selector = new DefaultMirrorSelector();
        for( Mirror mirror : m_settings.getMirrors() ) {
            selector
                .add( mirror.getName(), mirror.getUrl(), null, false, mirror.getMirrorOf(), "*" );
        }
        return selector;
    }

    private List<RemoteRepository> selectRepositories( Collection<Repository> repos ) {
        List<RemoteRepository> list = new ArrayList<RemoteRepository>();
        List<MavenRepositoryURL> urls = Collections.emptyList();
        try {
            urls = m_config.getRepositories();
        }
        catch( MalformedURLException exc ) {
            LOG.error( "invalid repository URLs", exc );
        }
        for( MavenRepositoryURL r : urls ) {
            if( r.isMulti() ) {
                addSubDirs( list, r.getFile() );
            }
            else {
                addRepo( list, r );
            }
        }
        
        return list;
    }
    
    private void addSubDirs( List<RemoteRepository> list, File parentDir ) {
        if( !parentDir.isDirectory() ) {
            LOG.debug( "Repository marked with @multi does not resolve to a directory: "
                + parentDir );
            return;
        }
        for( File repo : parentDir.listFiles() ) {
            if( repo.isDirectory() ) {
                try {
                    String repoURI = repo.toURI().toString() + "@id=" + repo.getName();
                    LOG.debug( "Adding repo from inside multi dir: " + repoURI );
                    addRepo( list, new MavenRepositoryURL( repoURI ) );
                }
                catch( MalformedURLException e ) {
                    LOG.error( "Error resolving repo url of a multi repo " + repo.toURI() );
                }
            }
        }
    }

    

    private void addRepo( List<RemoteRepository> list, MavenRepositoryURL repo ) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder( repo.getId(), REPO_TYPE, repo.getURL().toExternalForm() );
        RepositoryPolicy releasePolicy = new RepositoryPolicy( repo.isReleasesEnabled(), null, null );
        builder.setReleasePolicy( releasePolicy );
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy( repo.isSnapshotsEnabled(), null, null );
        builder.setSnapshotPolicy( snapshotPolicy );
        Authentication authentication = getAuthentication( repo.getId() );
        if (authentication != null) {
            builder.setAuthentication( authentication );
        }
        list.add( builder.build() );
    }
    
    /**
     * Resolve maven artifact as input stream.
     */
    public InputStream resolve( String groupId, String artifactId, String classifier,
        String extension, String version ) throws IOException {
        File resolved = resolveFile( groupId, artifactId, classifier, extension, version );
        return new FileInputStream( resolved );
    }

    /**
     * Resolve maven artifact as file in repository.
     */
    public File resolveFile( String groupId, String artifactId, String classifier,
        String extension, String version ) throws IOException {
        List<RemoteRepository> remoteRepos = selectRepositories( getRemoteRepositories( m_config ) );
        assignProxyAndMirrors( remoteRepos );
        // version = mapLatestToRange( version );
        RepositorySystemSession session = newSession();

        Artifact artifact = new DefaultArtifact( groupId, artifactId, classifier, extension,
            version );
        File resolved = resolve( session, remoteRepos, artifact );

        LOG.debug( "Resolved ({}) as {}", artifact.toString(), resolved.getAbsolutePath() );
        return resolved;
    }

    private File resolve( RepositorySystemSession session, List<RemoteRepository> remoteRepos,
        Artifact artifact ) throws IOException {
        try {
            artifact = resolveLatestVersionRange( session, remoteRepos, artifact );
            return m_repoSystem
                .resolveArtifact( session, new ArtifactRequest( artifact, remoteRepos, null ) )
                .getArtifact().getFile();
        }
        catch( ArtifactResolutionException e ) {
            /**
             * Do not add root exception to avoid NotSerializableException on DefaultArtifact. To
             * avoid loosing information log the root cause. We can remove this again as soon as
             * DefaultArtifact is serializeable. See http://team.ops4j.org/browse/PAXURL-206
             */
            LOG.warn( "Error resolving artifact" + artifact.toString() + ":" + e.getMessage(), e );
            throw new IOException( "Error resolving artifact " + artifact.toString() + ": "
                + e.getMessage() );
        }
        catch( RepositoryException e ) {
            throw new IOException( "Error resolving artifact " + artifact.toString(), e );
        }
    }

    /**
     * Tries to resolve versions = LATEST using an open range version query. If it succeeds, version
     * of artifact is set to the highest available version.
     * 
     * @param session
     *            to be used.
     * @param artifact
     *            to be used
     * 
     * @return an artifact with version set properly (highest if available)
     * 
     * @throws org.eclipse.aether.resolution.VersionRangeResolutionException
     *             in case of resolver errors.
     */
    private Artifact resolveLatestVersionRange( RepositorySystemSession session,
        List<RemoteRepository> remoteRepos, Artifact artifact )
        throws VersionRangeResolutionException {
        if( artifact.getVersion().equals( VERSION_LATEST ) ) {
            artifact = artifact.setVersion( LATEST_VERSION_RANGE );
        }

        VersionRangeResult versionResult = m_repoSystem.resolveVersionRange( session,
            new VersionRangeRequest( artifact, remoteRepos, null ) );
        if( versionResult != null ) {
            Version v = versionResult.getHighestVersion();
            if( v != null ) {
                
                artifact = artifact.setVersion( v.toString() );
            }
            else {
                throw new VersionRangeResolutionException( versionResult,
                    "No highest version found for " + artifact );
            }
        }
        return artifact;
    }

    private RepositorySystemSession newSession() {
        File local;
        if( m_config.getLocalRepository() == null ) {
            local = new File( System.getProperty( "user.home" ), ".m2/repository" );
        }
        else {
            local = m_config.getLocalRepository().getFile();
        }

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository( local );

        session.setLocalRepositoryManager( m_repoSystem.newLocalRepositoryManager( session,
            localRepo ) );
        session.setMirrorSelector( m_mirrorSelector );
        session.setProxySelector( m_proxySelector );

        String updatePolicy = m_config.getGlobalUpdatePolicy();
        if( null != updatePolicy ) {
            session.setUpdatePolicy( updatePolicy );
        }

        return session;
    }

    private Authentication getAuthentication( org.apache.maven.settings.Proxy proxy ) {
        // user, pass
        if( proxy.getUsername() != null ) {
            return new AuthenticationBuilder().addUsername( proxy.getUsername() )
                .addPassword( proxy.getPassword() ).build();
        }
        return null;
    }

    private Authentication getAuthentication( String repoId ) {
        Server server = m_settings.getServer( repoId );
        if (server != null && server.getUsername() != null) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername( server.getUsername() ).addPassword( server.getPassword() );
            return authBuilder.build();
        }
        return null;
    }

    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        locator.setServices( WagonProvider.class, new ManualWagonProvider( m_config.getTimeout() ) );
        locator.addService( TransporterFactory.class, WagonTransporterFactory.class );
        locator
            .addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );

        decrypter = new ConfigurableSettingsDecrypter();
        ConfigurableSecDispatcher secDispatcher = new ConfigurableSecDispatcher();
        try
        {
            secDispatcher.setCipher( new DefaultPlexusCipher() );
        }
        catch( PlexusCipherException exc )
        {
            throw new IllegalStateException( exc ); 
        }
        decrypter.setSecurityDispatcher( secDispatcher );
        
        locator.setServices( SettingsDecrypter.class, decrypter );
        
        
        
        locator.setService( LocalRepositoryManagerFactory.class,
            SimpleLocalRepositoryManagerFactory.class );
        locator.setService( org.eclipse.aether.spi.log.LoggerFactory.class,
            Slf4jLoggerFactory.class );

        return locator.getService( RepositorySystem.class );
    }
}
