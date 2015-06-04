/*
 * Copyright (C) 2010 Toni Menzel
 * Copyright (C) 2014 Guillaume Nodet
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

import static org.ops4j.pax.url.mvn.internal.Parser.VERSION_LATEST;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_FAIL;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_ALWAYS;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_DAILY;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_INTERVAL;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.slf4j.Slf4jLoggerFactory;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
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
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

/**
 * Aether based, drop in replacement for mvn protocol
 */
public class AetherBasedResolver implements MavenResolver {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger( AetherBasedResolver.class );
    private static final String LATEST_VERSION_RANGE = "(0.0,]";
    private static final String REPO_TYPE = "default";

    final private RepositorySystem m_repoSystem;
    final private MavenConfiguration m_config;
    final private MirrorSelector m_mirrorSelector;
    final private ProxySelector m_proxySelector;
    final private CloseableHttpClient m_client;
    private Settings m_settings;
    private ConfigurableSettingsDecrypter decrypter;

    private LocalRepository localRepository;
    private final ConcurrentMap<LocalRepository, Deque<RepositorySystemSession>> sessions
            = new ConcurrentHashMap<LocalRepository, Deque<RepositorySystemSession>>();

    /**
     * Create a AetherBasedResolver
     *
     * @param configuration (must be not null)
     */
    public AetherBasedResolver( final MavenConfiguration configuration ) {
        this( configuration, null );
    }

    /**
     * Create a AetherBasedResolver
     *
     * @param configuration (must be not null)
     */
    public AetherBasedResolver( final MavenConfiguration configuration, final Mirror mirror ) {
        NullArgumentException.validateNotNull( configuration, "Maven configuration");
        m_client = HttpClients.createClient(configuration.getPropertyResolver());
        m_config = configuration;
        m_settings = configuration.getSettings();
        m_repoSystem = newRepositorySystem();
        decryptSettings();
        m_proxySelector = selectProxies();
        m_mirrorSelector = selectMirrors( mirror );
    }

    @Override
    public void close() throws IOException {
        m_client.close();
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
        boolean aggregateReleaseEnabled = false, aggregateSnapshotEnabled = false;
        String aggregateReleaseUpdateInterval = null, aggregateSnapshotUpdateInterval = null;
        String aggregateReleaseChecksumPolicy = null, aggregateSnapshotChecksumPolicy = null; 

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

                // Aggregate policy settings of the mirror repos.
            	aggregateReleaseEnabled |= r.getPolicy( false ).isEnabled();
            	aggregateSnapshotEnabled |= r.getPolicy( true ).isEnabled();
            	aggregateReleaseUpdateInterval = minUpdateInterval( aggregateReleaseUpdateInterval, r.getPolicy( false ).getUpdatePolicy() );
            	aggregateSnapshotUpdateInterval = minUpdateInterval( aggregateSnapshotUpdateInterval, r.getPolicy( true ).getUpdatePolicy() );
            	aggregateReleaseChecksumPolicy = aggregateChecksumPolicy( aggregateReleaseChecksumPolicy, r.getPolicy( false ).getChecksumPolicy() );
            	aggregateSnapshotChecksumPolicy = aggregateChecksumPolicy( aggregateSnapshotChecksumPolicy, r.getPolicy( true ).getChecksumPolicy() );
            }
        }

        for( String mirrorId : map.keySet() ) {
            RemoteRepository mirror = naming.get( mirrorId );
            List<RemoteRepository> mirroredRepos = new ArrayList<RemoteRepository>();

            for( String rep : map.get( mirrorId ) ) {
                mirroredRepos.add( naming.get( rep ) );
            }
            RepositoryPolicy releasePolicy = new RepositoryPolicy(aggregateReleaseEnabled, aggregateReleaseUpdateInterval, aggregateReleaseChecksumPolicy);
            RepositoryPolicy snapshotPolicy = new RepositoryPolicy(aggregateSnapshotEnabled, aggregateSnapshotUpdateInterval, aggregateSnapshotChecksumPolicy);
            mirror = new RemoteRepository.Builder( mirror ).setMirroredRepositories( mirroredRepos )
                .setProxy( m_proxySelector.getProxy( mirror ) )
                .setReleasePolicy( releasePolicy )
                .setSnapshotPolicy( snapshotPolicy )
                .build();
            resultingRepos.removeAll( mirroredRepos );
            resultingRepos.add( 0, mirror );
        }

        remoteRepos.clear();
        remoteRepos.addAll( resultingRepos );
    }

    private String minUpdateInterval(String interval1, String interval2) {
    	LOG.warn("interval1: {}, interval2: {}", interval1, interval2);
    	if( interval1 == null ) {
    		return interval2;
    	} else if( interval2 == null ) {
    		return interval1;
    	}

    	int interval1InMin = getIntervalInMinutes( interval1 );
    	int interval2InMin = getIntervalInMinutes( interval2 );
    	if( interval1InMin <= interval2InMin ) {
    		return getUpdatePolicyInterval( interval1InMin );
    	} else {
    		return getUpdatePolicyInterval( interval2InMin );
    	}
    }

    private int getIntervalInMinutes(String interval) {
    	int intervalInMin;
    	if (interval.equals( UPDATE_POLICY_NEVER )) {
    		intervalInMin = Integer.MAX_VALUE;
    	} else if (interval.equals( UPDATE_POLICY_DAILY )) {
    		intervalInMin = 24 * 60;
    	} else if (interval.equals( UPDATE_POLICY_ALWAYS )) {
    		intervalInMin = Integer.MIN_VALUE;
    	} else if (interval.startsWith( UPDATE_POLICY_INTERVAL + ":")) {
    		try {
				intervalInMin = Integer.parseInt(interval.substring( UPDATE_POLICY_INTERVAL.length() + 1 ));
			} catch (NumberFormatException e) {
				LOG.warn("unable to parse update policy interval: \"{}\"", interval);
				intervalInMin = 24 * 60;
			}
    	} else {
    		throw new IllegalArgumentException( String.format( "Invalid update policy \"%s\"", interval ) );
    	}
    	return intervalInMin;
    }

    private String getUpdatePolicyInterval(int intervalInMin) {
    	switch (intervalInMin) {
    	case Integer.MAX_VALUE:
    		return UPDATE_POLICY_NEVER;
    	case Integer.MIN_VALUE:
    		return UPDATE_POLICY_ALWAYS;
    	case 24 * 60:
    		return UPDATE_POLICY_DAILY;
    	default:
    		return String.format("%s:%d", UPDATE_POLICY_INTERVAL, intervalInMin);
    	}
    }

    private String aggregateChecksumPolicy( String policy1, String policy2 ) {
    	if( policy1 == null ) {
    		return policy2;
    	}
    	if( policy2 == null ) {
    		return policy1;
    	}
    	if( policy1.equals( CHECKSUM_POLICY_FAIL ) || policy2.equals( CHECKSUM_POLICY_FAIL ) ) {
    		return CHECKSUM_POLICY_FAIL;
    	} else if( policy1.equals( CHECKSUM_POLICY_WARN ) || policy2.equals( CHECKSUM_POLICY_WARN ) ) {
    		return CHECKSUM_POLICY_WARN;
    	} else {
    		return CHECKSUM_POLICY_IGNORE;
    	}
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

    private MirrorSelector selectMirrors( Mirror mirror ) {
        // configure mirror

       	// The class org.eclipse.aether.util.repository.DefaultMirrorSelector is final therefore it needs to be
    	// wrapped to fix PAXURL-289.
    	class DefaultMirrorSelectorWrapper implements MirrorSelector {
            final DefaultMirrorSelector delegate = new DefaultMirrorSelector();
            final Map<String, Authentication> authMap = new HashMap<String, Authentication>();

			@Override
			public RemoteRepository getMirror(RemoteRepository repository) {
				RemoteRepository repo = delegate.getMirror( repository );
				if( repo != null ) {
					Authentication mirrorAuth = authMap.get( repo.getId() );
					if( mirrorAuth != null ) {
						RemoteRepository.Builder builder = new RemoteRepository.Builder( repo );
						repo = builder.setAuthentication( mirrorAuth ).build();
					}
				}
				return repo;
			}
			
			public DefaultMirrorSelector add( String id, String url, String type, boolean repositoryManager,
					String mirrorOfIds, String mirrorOfTypes, Authentication authentication ) {
				LOG.trace("adding mirror {} auth = {}", id, authentication != null);
				if( authentication != null ) {					
					authMap.put(id, authentication);
				}
				return delegate.add( id, url, type, repositoryManager, mirrorOfIds, mirrorOfTypes );
			}
    	}

    	DefaultMirrorSelectorWrapper selector = new DefaultMirrorSelectorWrapper();
        for( Mirror m : m_settings.getMirrors() ) {
            selector.add( m.getName(), m.getUrl(), null, false, m.getMirrorOf(), "*", getAuthentication( m.getId() ) );
        }
        if( mirror != null ) {
            selector.add(mirror.getName(), mirror.getUrl(), null, false, mirror.getMirrorOf(), "*", getAuthentication( mirror.getId() ) );
        }
        return selector;
    }

    private List<RemoteRepository> selectRepositories() {
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

    List<LocalRepository> selectDefaultRepositories() {
        List<LocalRepository> list = new ArrayList<LocalRepository>();
        List<MavenRepositoryURL> urls = Collections.emptyList();
        try {
            urls = m_config.getDefaultRepositories();
        }
        catch( MalformedURLException exc ) {
            LOG.error( "invalid repository URLs", exc );
        }
        for( MavenRepositoryURL r : urls ) {
            if( r.isMulti() ) {
                addLocalSubDirs(list, r.getFile());
            }
            else {
                addLocalRepo(list, r);
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
        String releasesUpdatePolicy = repo.getReleasesUpdatePolicy();
        if (releasesUpdatePolicy == null || releasesUpdatePolicy.isEmpty()) {
            releasesUpdatePolicy = UPDATE_POLICY_DAILY;
        }
        String releasesChecksumPolicy = repo.getReleasesChecksumPolicy();
        if (releasesChecksumPolicy == null || releasesChecksumPolicy.isEmpty()) {
            releasesChecksumPolicy = CHECKSUM_POLICY_WARN;
        }
        String snapshotsUpdatePolicy = repo.getSnapshotsUpdatePolicy();
        if (snapshotsUpdatePolicy == null || snapshotsUpdatePolicy.isEmpty()) {
            snapshotsUpdatePolicy = UPDATE_POLICY_DAILY;
        }
        String snapshotsChecksumPolicy = repo.getSnapshotsChecksumPolicy();
        if (snapshotsChecksumPolicy == null || snapshotsChecksumPolicy.isEmpty()) {
            snapshotsChecksumPolicy = CHECKSUM_POLICY_WARN;
        }
        RemoteRepository.Builder builder = new RemoteRepository.Builder( repo.getId(), REPO_TYPE, repo.getURL().toExternalForm() );
        RepositoryPolicy releasePolicy = new RepositoryPolicy( repo.isReleasesEnabled(), releasesUpdatePolicy, releasesChecksumPolicy );
        builder.setReleasePolicy( releasePolicy );
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy( repo.isSnapshotsEnabled(), snapshotsUpdatePolicy, snapshotsChecksumPolicy );
        builder.setSnapshotPolicy( snapshotPolicy );
        Authentication authentication = getAuthentication( repo.getId() );
        if (authentication != null) {
            builder.setAuthentication( authentication );
        }
        list.add( builder.build() );
    }
    
    private void addLocalSubDirs( List<LocalRepository> list, File parentDir ) {
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
                    addLocalRepo(list, new MavenRepositoryURL(repoURI));
                }
                catch( MalformedURLException e ) {
                    LOG.error( "Error resolving repo url of a multi repo " + repo.toURI() );
                }
            }
        }
    }

    private void addLocalRepo( List<LocalRepository> list, MavenRepositoryURL repo ) {
        if (repo.getFile() != null) {
            LocalRepository local = new LocalRepository( repo.getFile(), "simple" );
            list.add( local );
        }
    }

    public RepositorySystem getRepositorySystem() {
        return m_repoSystem;
    }

    public List<RemoteRepository> getRepositories() {
        List<RemoteRepository> repos = selectRepositories();
        assignProxyAndMirrors(repos);
        return repos;
    }

    public File resolve(String url) throws IOException {
        if (!url.startsWith(ServiceConstants.PROTOCOL + ":")) {
            throw new IllegalArgumentException("url should be a mvn based url");
        }
        url = url.substring((ServiceConstants.PROTOCOL + ":").length());
        Parser parser = new Parser(url);
        return resolve(
                parser.getGroup(),
                parser.getArtifact(),
                parser.getClassifier(),
                parser.getType(),
                parser.getVersion(),
                parser.getRepositoryURL()
        );
    }

    /**
     * Resolve maven artifact as file in repository.
     */
    public File resolve( String groupId, String artifactId, String classifier,
                             String extension, String version ) throws IOException {
        return resolve(groupId, artifactId, classifier, extension, version, null);
    }

    /**
     * Resolve maven artifact as file in repository.
     */
    public File resolve( String groupId, String artifactId, String classifier,
                             String extension, String version,
                             MavenRepositoryURL repositoryURL ) throws IOException {
        Artifact artifact = new DefaultArtifact( groupId, artifactId, classifier, extension, version );
        return resolve(artifact, repositoryURL);
    }

    /**
     * Resolve maven artifact as file in repository.
     */
    public File resolve( Artifact artifact ) throws IOException {
        return resolve(artifact, null);
    }

    /**
     * Resolve maven artifact as file in repository.
     */
    public File resolve( Artifact artifact,
                         MavenRepositoryURL repositoryURL ) throws IOException {

        List<LocalRepository> defaultRepos = selectDefaultRepositories();
        List<RemoteRepository> remoteRepos = Collections.EMPTY_LIST;
        if (repositoryURL == null || !repositoryURL.useOnlyLocalRepositories()) {
            remoteRepos = selectRepositories();
            if (repositoryURL != null) {
                addRepo(remoteRepos, repositoryURL);
            }
            assignProxyAndMirrors(remoteRepos);
        }//else not url specified or only local onces so keep going
        File resolved = resolve( defaultRepos, remoteRepos, artifact );

        LOG.debug( "Resolved ({}) as {}", artifact.toString(), resolved.getAbsolutePath() );
        return resolved;
    }

    private File resolve( List<LocalRepository> defaultRepos,
                          List<RemoteRepository> remoteRepos,
                          Artifact artifact ) throws IOException {

        if (artifact.getExtension().isEmpty()) {
            artifact = new DefaultArtifact(
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getClassifier(),
                    "jar",
                    artifact.getVersion()
            );
        }

        // Try with default repositories
        try {
            VersionConstraint vc = new GenericVersionScheme().parseVersionConstraint(artifact.getVersion());
            if (vc.getVersion() != null) {
                for (LocalRepository repo : defaultRepos) {
                    RepositorySystemSession session = newSession( repo );
                    try {
                        return m_repoSystem
                                .resolveArtifact(session, new ArtifactRequest(artifact, null, null))
                                .getArtifact().getFile();
                    }
                    catch( ArtifactResolutionException e ) {
                        // Ignore
                    } finally {
                        releaseSession(session);
                    }
                }
            }
        }
        catch( InvalidVersionSpecificationException e ) {
            // Should not happen
        }
        RepositorySystemSession session = newSession( null );
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
        } finally {
            releaseSession(session);
        }
    }

    @Override
    public File resolveMetadata(String groupId, String artifactId, String type, String version) throws IOException {
        RepositorySystem system = getRepositorySystem();
        RepositorySystemSession session = newSession();
        try {
            Metadata metadata = new DefaultMetadata(groupId, artifactId, version,
                                                    type, Metadata.Nature.RELEASE_OR_SNAPSHOT);
            List<MetadataRequest> requests = new ArrayList<MetadataRequest>();
            for (RemoteRepository repository : getRepositories()) {
                MetadataRequest request = new MetadataRequest(metadata, repository, null);
                request.setFavorLocalRepository(false);
                requests.add(request);
            }
            MetadataRequest request = new MetadataRequest(metadata, null, null);
            request.setFavorLocalRepository(true);
            requests.add(request);
            org.apache.maven.artifact.repository.metadata.Metadata mr = new org.apache.maven.artifact.repository.metadata.Metadata();
            mr.setModelVersion("1.1.0");
            mr.setGroupId(metadata.getGroupId());
            mr.setArtifactId(metadata.getArtifactId());
            mr.setVersioning(new Versioning());
            boolean merged = false;
            List<MetadataResult> results = system.resolveMetadata(session, requests);
            for (MetadataResult result : results) {
                if (result.getMetadata() != null && result.getMetadata().getFile() != null) {
                    FileInputStream fis = new FileInputStream(result.getMetadata().getFile());
                    org.apache.maven.artifact.repository.metadata.Metadata m = new MetadataXpp3Reader().read(fis, false);
                    fis.close();
                    if (m.getVersioning() != null) {
                        mr.getVersioning().setLastUpdated(latestTimestamp(mr.getVersioning().getLastUpdated(), m.getVersioning().getLastUpdated()));
                        mr.getVersioning().setLatest(latestVersion(mr.getVersioning().getLatest(), m.getVersioning().getLatest()));
                        mr.getVersioning().setRelease(latestVersion(mr.getVersioning().getRelease(), m.getVersioning().getRelease()));
                        for (String v : m.getVersioning().getVersions()) {
                            if (!mr.getVersioning().getVersions().contains(v)) {
                                mr.getVersioning().getVersions().add(v);
                            }
                        }
                        mr.getVersioning().getSnapshotVersions().addAll(m.getVersioning().getSnapshotVersions());
                    }
                    merged = true;
                }
            }
            if (merged) {
                Collections.sort(mr.getVersioning().getVersions(), VERSION_COMPARATOR);
                Collections.sort(mr.getVersioning().getSnapshotVersions(), SNAPSHOT_VERSION_COMPARATOR);
                File tmpFile = Files.createTempFile("mvn-", ".tmp").toFile();
                FileOutputStream fos = new FileOutputStream(tmpFile);
                try {
                    new MetadataXpp3Writer().write(fos, mr);
                } finally {
                    fos.close();
                }
                return tmpFile;
            }
            return null;
        } catch (Exception e) {
            throw new IOException("Unable to resolve metadata", e);
        } finally {
            releaseSession(session);
        }
    }

    @Override
    public void upload(String groupId, String artifactId, String classifier, String extension, String version, File file) throws IOException {
        RepositorySystem system = getRepositorySystem();
        RepositorySystemSession session = newSession();
        try {
            Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version,
                                                    null, file);
            InstallRequest request = new InstallRequest();
            request.addArtifact(artifact);
            system.install(session, request);
        } catch (Exception e) {
            throw new IOException("Unable to install artifact", e);
        } finally {
            releaseSession(session);
        }
    }

    @Override
    public void uploadMetadata(String groupId, String artifactId, String type, String version, File file) throws IOException {
        RepositorySystem system = getRepositorySystem();
        RepositorySystemSession session = newSession();
        try {
            Metadata metadata = new DefaultMetadata(groupId, artifactId, version,
                                                    type, Metadata.Nature.RELEASE_OR_SNAPSHOT,
                                                    file);
            InstallRequest request = new InstallRequest();
            request.addMetadata(metadata);
            system.install(session, request);
        } catch (Exception e) {
            throw new IOException("Unable to install metadata", e);
        } finally {
            releaseSession(session);
        }
    }

    private Comparator<String> VERSION_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String v1, String v2) {
            try {
                Version vv1 = new GenericVersionScheme().parseVersion(v1);
                Version vv2 = new GenericVersionScheme().parseVersion(v2);
                return vv1.compareTo(vv2);
            } catch (Exception e) {
                return v1.compareTo(v2);
            }
        }
    };

    private Comparator<SnapshotVersion> SNAPSHOT_VERSION_COMPARATOR = new Comparator<SnapshotVersion>() {
        @Override
        public int compare(SnapshotVersion o1, SnapshotVersion o2) {
            int c = VERSION_COMPARATOR.compare(o1.getVersion(), o2.getVersion());
            if (c == 0) {
                c = o1.getExtension().compareTo(o2.getExtension());
            }
            if (c == 0) {
                c = o1.getClassifier().compareTo(o2.getClassifier());
            }
            return c;
        }
    };

    private String latestTimestamp(String t1, String t2) {
        if (t1 == null) {
            return t2;
        } else if (t2 == null) {
            return t1;
        }  else {
            return t1.compareTo(t2) < 0 ? t2 : t1;
        }
    }

    private String latestVersion(String v1, String v2) {
        if (v1 == null) {
            return v2;
        } else if (v2 == null) {
            return v1;
        } else {
            return VERSION_COMPARATOR.compare(v1, v2) < 0 ? v2 : v1;
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

    public RepositorySystemSession newSession() {
        return newSession( null );
    }

    private RepositorySystemSession newSession(LocalRepository repo) {
        if (repo == null) {
            if (localRepository == null) {
                File local;
                if( m_config.getLocalRepository() != null ) {
                    local = m_config.getLocalRepository().getFile();
                } else {
                    local = new File( System.getProperty( "user.home" ), ".m2/repository" );
                }
                localRepository = new LocalRepository( local );
            }
            repo = localRepository;
        }
        Deque<RepositorySystemSession> deque = sessions.get(repo);
        RepositorySystemSession session = null;
        if (deque != null) {
            session = deque.pollFirst();
        }
        if (session == null) {
            session = createSession(repo);
        }
        return session;
    }

    private void releaseSession(RepositorySystemSession session) {
        LocalRepository repo = session.getLocalRepository();
        Deque<RepositorySystemSession> deque = sessions.get(repo);
        if (deque == null) {
            sessions.putIfAbsent(repo, new ConcurrentLinkedDeque<RepositorySystemSession>());
            deque = sessions.get(repo);
        }
        deque.add(session);
    }

    private RepositorySystemSession createSession(LocalRepository repo) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        if( repo != null ) {
            session.setLocalRepositoryManager( m_repoSystem.newLocalRepositoryManager( session, repo ) );
        } else {
            File local;
            if( m_config.getLocalRepository() != null ) {
                local = m_config.getLocalRepository().getFile();
            } else {
                local = new File( System.getProperty( "user.home" ), ".m2/repository" );
            }
            LocalRepository localRepo = new LocalRepository( local );
            session.setLocalRepositoryManager( m_repoSystem.newLocalRepositoryManager( session, localRepo ) );
        }

        session.setMirrorSelector( m_mirrorSelector );
        session.setProxySelector( m_proxySelector );

        String updatePolicy = m_config.getGlobalUpdatePolicy();
        if( null != updatePolicy ) {
            session.setUpdatePolicy( updatePolicy );
        }
        
        for (Server server : m_settings.getServers()) {
            if (server.getConfiguration() != null
                && ((Xpp3Dom)server.getConfiguration()).getChild("httpHeaders") != null) {
                addServerConfig(session, server);
            }
        }

        session.setOffline( m_config.isOffline() );

        return session;
    }

    private void addServerConfig( DefaultRepositorySystemSession session, Server server )
    {
        Map<String,String> headers = new HashMap<String, String>();
        Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        Xpp3Dom httpHeaders = configuration.getChild( "httpHeaders" );
        for (Xpp3Dom httpHeader : httpHeaders.getChildren( "httpHeader" )) {
            Xpp3Dom name = httpHeader.getChild( "name" );
            String headerName = name.getValue();
            Xpp3Dom value = httpHeader.getChild( "value" );
            String headerValue = value.getValue();
            headers.put( headerName, headerValue );
        }
        session.setConfigProperty( String.format("%s.%s", ConfigurationProperties.HTTP_HEADERS, server.getId() ), headers );
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

        locator.setServices( WagonProvider.class, new ManualWagonProvider( m_client, m_config.getTimeout() ) );
        locator.addService( TransporterFactory.class, WagonTransporterFactory.class );
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);

        decrypter = new ConfigurableSettingsDecrypter();
        PaxUrlSecDispatcher secDispatcher = new PaxUrlSecDispatcher();
        try
        {
            secDispatcher.setCipher( new DefaultPlexusCipher() );
        }
        catch( PlexusCipherException exc )
        {
            throw new IllegalStateException( exc ); 
        }
        secDispatcher.setConfigurationFile( m_config.getSecuritySettings() );
        decrypter.setSecurityDispatcher( secDispatcher );
        
        locator.setServices( SettingsDecrypter.class, decrypter );
        
        
        
        locator.setService( LocalRepositoryManagerFactory.class,
            SimpleLocalRepositoryManagerFactory.class );
        locator.setService( org.eclipse.aether.spi.log.LoggerFactory.class,
            Slf4jLoggerFactory.class );

        return locator.getService( RepositorySystem.class );
    }
}
