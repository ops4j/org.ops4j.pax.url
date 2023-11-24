/*
 * Copyright 2010 Toni Menzel.
 * Copyright 2014 Guillaume Nodet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.MetadataTransferException;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MirrorInfo;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_DAILY;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import static org.ops4j.pax.url.mvn.internal.Parser.VERSION_LATEST;

/**
 * <p>{@link MavenResolver} based on <a href="https://maven.apache.org/resolver/">Apache Maven Resolver</a>
 * (formerly - <a href="https://projects.eclipse.org/projects/technology.aether">Eclipse Aether Resolver</a>
 * (formerly - <a href="https://github.com/sonatype/sonatype-aether">Sonatype Aether Resolver</a>)).</p>
 *
 * <p>The resolution process is almost the same as in usual {@code mvn} invocation with one big difference. While
 * in pure Maven, remote repositories are used to download the artifacts to local repository if not already available,
 * in Pax URL Aether, there's an initial resolution attempt based on <em>default repositories</em> which are treated
 * as local repositories without write access. This is important for managing separate directories with Maven-like
 * structure - for example in {@code $KARAF_HOME/system}.</p>
 */
public class AetherBasedResolver implements MavenResolver {

    private static final Logger LOG = LoggerFactory.getLogger(AetherBasedResolver.class);

    private static final String LATEST_VERSION_RANGE = "[0.0,)";

    // type for LocalRepository managed by EnhancedLocalRepositoryManager
    private static final String ENHANCED_REPOSITORY_TYPE = "default";

    // see org.eclipse.aether.internal.impl.DefaultUpdateCheckManager#SESSION_CHECKS
    private static final String SESSION_CHECKS = "updateCheckManager.checks";

    // configuration changes with every PID update, but then it's effectively immutable
    final private MavenConfiguration m_config;
    // decrypted Maven settings loaded from default location or location specified in the config
    final private Settings m_settings;
    // local repository instance obtained from the settings or configuration with fallback ~/.m2/repository
    private final LocalRepository m_localRepository;

    // whether to use properties like http.proxyHost (and related) here and in HttpClient4
    // (maven-resolver-transport-http)
    boolean m_useSystemProperties;

    // main entry-point to Maven Resolver. Since Pax URL 3 it is configured using MiMa and
    // new maven-resolver-supplier (org.eclipse.aether.supplier.RepositorySystemSupplier)
    final private RepositorySystem m_repoSystem;
    // default session used to apply mirrors and proxies when there's no other session
    final private RepositorySystemSession m_defaultSession;

    final private MirrorSelector m_mirrorSelector;
    final private ProxySelector m_proxySelector;
    final private AuthenticationSelector m_authenticationSelector;

    // cache of sessions per local repository
    private final ConcurrentMap<LocalRepository, Deque<RepositorySystemSession>> sessions = new ConcurrentHashMap<>();

    private final AtomicBoolean m_shutdown = new AtomicBoolean(false);

    /**
     * Create a AetherBasedResolver
     *
     * @param configuration (must be not null)
     */
    public AetherBasedResolver(final MavenConfiguration configuration) {
        this(configuration, null);
    }

    /**
     * Create a AetherBasedResolver
     *
     * @param configuration (must be not null)
     * @param mirror
     */
    public AetherBasedResolver(final MavenConfiguration configuration, final MirrorInfo mirror) {
        NullArgumentException.validateNotNull(configuration, "Maven configuration");

        m_config = configuration;
        // these should be already decrypted
        m_settings = configuration.getSettings();

        File localRepository = m_config.getLocalRepository();
        if (localRepository != null) {
            m_localRepository = new LocalRepository(localRepository, ENHANCED_REPOSITORY_TYPE);
        } else {
            m_localRepository = null;
        }

        m_useSystemProperties = m_config.getProperty(ServiceConstants.PROPERTY_USE_SYSTEM_PROPERTIES, false, Boolean.TYPE);

        // create global, thread-safe org.eclipse.aether.RepositorySystem used for all resolution operations
        m_repoSystem = newRepositorySystem();

        // proxy and mirror selectors are thread-safe objects configured once, but used for every created session
        // org.eclipse.aether.internal.impl.DefaultRepositorySystem.newResolutionRepositories() is actually
        // doing all the work which is quite simple and clear:
        //  - each remote repository used in resolution is potentially replaced by matching mirror
        //  - org.eclipse.aether.repository.Authentication is obtained for a repository (or alternative mirror)
        //    and added to the repository
        //  - org.eclipse.aether.repository.Proxy is obtained for a repository (or alternative mirror)
        //    and added to the repository (proxy may have own, separate Authentication)
        m_proxySelector = configureProxySelector();
        m_mirrorSelector = configureMirrorSelector(mirror);
        m_authenticationSelector = configureGlobalAuthenticationSelector();

        // create singleton session used only for org.eclipse.aether.RepositorySystem.newResolutionRepositories()
        // call when session is null
        m_defaultSession = newDefaultRepositorySystemSession();
    }

    @Override
    public void close() throws IOException {
        // https://github.com/ops4j/org.ops4j.pax.url/issues/417#issuecomment-1812559451
        m_repoSystem.shutdown();
        m_shutdown.set(true);
        sessions.clear();
    }

    // ---- configuration methods invoked from constructor

    /**
     * Configures Maven Resolver's {@link ProxySelector} with proxies from settings.xml.
     *
     * @return
     */
    private ProxySelector configureProxySelector() {
        DefaultProxySelector proxySelector = new DefaultProxySelector();

        for (org.apache.maven.settings.Proxy proxy : m_settings.getProxies()) {
            if (!proxy.isActive()) {
                continue;
            }
            String nonProxyHosts = proxy.getNonProxyHosts();
            Authentication auth = getProxyAuthentication(proxy);
            Proxy proxyObj = new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
            proxySelector.add(proxyObj, nonProxyHosts);
        }

        // don't configure org.eclipse.aether.repository.Proxy based on http.proxyHost (and related). These
        // properties will be configured at maven-resolver-transport-http level when
        // "aether.connector.http.useSystemProperties" is found in org.eclipse.aether.transport.http.HttpTransporter.HttpTransporter

        return proxySelector;
    }

    /**
     * Returns {@link Authentication} for proxy configured in Maven settings.xml
     *
     * @param proxy
     * @return
     */
    private Authentication getProxyAuthentication(org.apache.maven.settings.Proxy proxy) {
        if (proxy.getUsername() != null) {
            return new AuthenticationBuilder()
                    .addUsername(proxy.getUsername()).addPassword(proxy.getPassword())
                    .build();
        }
        return null;
    }

    /**
     * <p>Configures Maven Resolver's {@link MirrorSelector} with mirros from settings.xml, manual mirror information and
     * possibly {@code mavenMirrorUrl} system property or {@code MAVEN_MIRROR_URL} environment variable.</p>
     *
     * <p>Pax URL 2 configured mirror authentication data here, but it's no longer necessary</p>
     *
     * @param mirror
     * @return
     */
    private MirrorSelector configureMirrorSelector(MirrorInfo mirror) {
        final DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();

        for (Mirror m : m_settings.getMirrors()) {
            mirrorSelector.add(m.getId(), m.getUrl(), null, false, m.isBlocked(), m.getMirrorOf(), m.getMirrorOfLayouts());
        }
        // additional mirror, but I didn't find any usage of this (here or in Karaf for example)
        if (mirror != null) {
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), null, false, false, mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
        }

        return mirrorSelector;
    }

    /**
     * Configures {@link AuthenticationSelector} just like in {@link StandaloneRuntimeSupport}, which takes
     * the credentials from decrypted {@link Settings#getServers()}. Only servers from settings.xml are
     * used.
     *
     * @return
     */
    private AuthenticationSelector configureGlobalAuthenticationSelector() {
        DefaultAuthenticationSelector defaultSelector = new DefaultAuthenticationSelector();

        // settings should already be decrypted
        for (Server server : m_settings.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            defaultSelector.add(server.getId(), authBuilder.build());
        }

        // this is needed, so if RemoteRepository has Authentication already attached, it'll be used.
        // this is special case for these MavenRepositoryURLs which use user@password part of URI authority
        return new ConservativeAuthenticationSelector(defaultSelector);
    }

    // ---- main resolution methods
    //      the versions with "previousException" are used for example by
    //      org.apache.karaf.features.internal.download.impl.MavenDownloadTask to make retryable process smarter

    @Override
    public File resolve(String url) throws IOException {
        return resolve(url, null);
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        if (!url.startsWith(ServiceConstants.PROTOCOL + ":")) {
            throw new IllegalArgumentException("url should be a mvn based url");
        }
        url = url.substring(4);
        Parser parser = new Parser(url);
        return resolve(parser.getGroup(), parser.getArtifact(), parser.getClassifier(), parser.getType(), parser.getVersion(), parser.getRepositoryURL(), previousException);
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version) throws IOException {
        return resolve(groupId, artifactId, classifier, extension, version, null, null);
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version, Exception previousException) throws IOException {
        return resolve(groupId, artifactId, classifier, extension, version, null, previousException);
    }

    /**
     * Resolve artifact, possibly checking extra (comparing to configured repositories)
     * {@link MavenRepositoryURL} obtained from {@code mvn:} URI.
     *
     * @param groupId
     * @param artifactId
     * @param classifier
     * @param extension
     * @param version
     * @param url additional Maven Repository URL to check
     * @param previousException
     * @return
     * @throws IOException
     */
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version,
            MavenRepositoryURL url, Exception previousException) throws IOException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        return resolve(artifact, url, previousException);
    }

    /**
     * Main resolution method that resolves artifact using data from {@link Artifact} and repositories
     * prepared from global configuration.
     *
     * @param artifact
     * @param url extra URL for remote repository to check if the original {@code mvn:} URI included source repository
     * @param previousException
     * @return
     * @throws IOException
     */
    public File resolve(Artifact artifact, MavenRepositoryURL url, Exception previousException) throws IOException {

        List<LocalRepositoryWithConfig> defaultRepositories = selectDefaultRepositories();
        List<RemoteRepository> remoteRepositories = selectRemoteRepositories(url);

        // PAXURL-337: use previousException as hint to alter remote repositories to query
        if (previousException != null) {
            // we'll try using previous repositories, without these that will fail again anyway
            List<RemoteRepository> altered = new LinkedList<>();
            RepositoryException repositoryException = findAetherException(previousException);
            if (repositoryException instanceof ArtifactResolutionException) {
                // check only this aggregate exception and assume it's related to current artifact
                ArtifactResult result = ((ArtifactResolutionException) repositoryException).getResult();
                if (result != null && result.getRequest() != null && result.getRequest().getArtifact().equals(artifact)) {
                    // one exception per repository checked
                    // consider only ArtifactTransferException:
                    //  - they may be recoverable
                    //  - these exceptions contain repository that was checked
                    for (Exception exception : result.getExceptions()) {
                        RepositoryException singleException = findAetherException(exception);
                        if (singleException instanceof ArtifactTransferException) {
                            RemoteRepository repository = ((ArtifactTransferException) singleException).getRepository();
                            if (repository != null) {
                                RetryChance chance = isRetryableException(singleException);
                                if (chance == RetryChance.NEVER) {
                                    LOG.debug("Removing " + repository + " from list of repositories, previous exception: " +
                                            singleException.getClass().getName() + ": " + singleException.getMessage());
                                } else {
                                    altered.add(repository);
                                }
                            }
                        }
                    }

                    // swap list of repos now
                    remoteRepositories = altered;
                }
            }
        }

        File resolved = resolve(defaultRepositories, remoteRepositories, artifact);

        if (resolved != null) {
            LOG.debug("Resolved ({}) as {}", artifact.toString(), resolved.getAbsolutePath());
        }

        return resolved;
    }

    /**
     * <p>Main resolution method that operates on passed <em>default</em> and <em>remote</em> repositories and
     * doesn't determine the checked repositories on its own.</p>
     *
     * <p>This is the main method that implements Pax URL resolution, which involves a set of <em>default</em>
     * repositories and defaults to remote repository resolution if artifact is not found.</p>
     *
     * <p>This method requires that {@link RemoteRepository} objects used are already processed (mirrors,
     * proxies, auth).</p>
     *
     * @param defaultRepositories
     * @param remoteRepositories
     * @param artifact
     * @return
     * @throws IOException
     */
    public File resolve(List<LocalRepositoryWithConfig> defaultRepositories, List<RemoteRepository> remoteRepositories,
            Artifact artifact) throws IOException {

        if (artifact.getExtension().isEmpty()) {
            artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                    "jar", artifact.getVersion()
            );
        }

        if (artifact.getVersion().equals(VERSION_LATEST)) {
            artifact = artifact.setVersion(LATEST_VERSION_RANGE);
        }

        // This is where Pax URL Aether does its 2-stage default+remote repositories resolution

        // 1). Try with default repositories first. These are normal Maven local repositories and resolution
        // is performed for each of them, passing empty list of remote repositories
        try {
            GenericVersionScheme genericVersionScheme = new GenericVersionScheme();
            VersionConstraint vc = genericVersionScheme.parseVersionConstraint(artifact.getVersion());

            // first, each "default repo" will be treated as local repo and resolution will be performed
            // without remote repositories
            for (LocalRepositoryWithConfig repo : defaultRepositories) {
                RepositorySystemSession session = findOrCreateSession(repo);
                if (session == null) {
                    throw new IllegalStateException("No session configured for default repository " + repo);
                }
                try {
                    if (vc.getVersion() == null && vc.getRange() != null) {
                        // KARAF-6005: try to resolve version range against local repository (default repository)
                        Metadata metadata =
                                new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(),
                                        "maven-metadata.xml", Metadata.Nature.RELEASE_OR_SNAPSHOT);

                        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
                        // this already takes "split" configuration into account
                        String path = lrm.getPathForLocalMetadata(metadata);
                        File metadataLocation = new File(lrm.getRepository().getBasedir(), path).getParentFile();

                        Set<Version> versions = new TreeSet<>();
                        if (metadataLocation.isDirectory()) {
                            if (!new File(metadataLocation, "maven-metadata.xml").isFile()) {
                                // we will generate (kind of) maven-metadata.xml manually
                                String[] versionDirs = metadataLocation.list();
                                if (versionDirs != null) {
                                    for (String vd : versionDirs) {
                                        Version ver = genericVersionScheme.parseVersion(vd);
                                        if (vc.containsVersion(ver)) {
                                            versions.add(ver);
                                        }
                                    }
                                }
                                VersionRangeResult vrr = new VersionRangeResult(new VersionRangeRequest());
                                vrr.setVersions(new LinkedList<>(versions));

                                if (vrr.getHighestVersion() != null) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Resolved version range {} as {}", vc.getRange(), vrr.getHighestVersion().toString());
                                    }
                                    vc = new GenericVersionScheme().parseVersionConstraint(vrr.getHighestVersion().toString());
                                    artifact = artifact.setVersion(vc.getVersion().toString());
                                }
                            } else {
                                // we can use normal metadata resolution algorithm
                                try {
                                    VersionRangeResult versionResult = m_repoSystem.resolveVersionRange(session,
                                            new VersionRangeRequest(artifact, null, null));
                                    if (versionResult != null) {
                                        Version v = versionResult.getHighestVersion();
                                        if (v != null) {
                                            if (LOG.isDebugEnabled()) {
                                                LOG.debug("Resolved version range {} as {}", vc.getRange(), v.toString());
                                            }
                                            vc = new GenericVersionScheme().parseVersionConstraint(v.toString());
                                            artifact = artifact.setVersion(vc.getVersion().toString());
                                        }
                                    }
                                } catch (VersionRangeResolutionException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                    if (vc.getVersion() != null) {
                        // normal resolution without ranges
                        try {
                            return m_repoSystem
                                    .resolveArtifact(session, new ArtifactRequest(artifact, null, null))
                                    .getArtifact().getFile();
                        } catch (ArtifactResolutionException e) {
                            // Ignore
                        }
                    }
                } finally {
                    releaseSession(session);
                }
            }
        } catch (InvalidVersionSpecificationException e) {
            // Should not happen
        }

        // 2). Resolve using collection of remote repositories with single local repository as cache
        RepositorySystemSession session = findOrCreateSession(null);

        if (session == null) {
            LOG.debug("Skipping remote repository resolution - no local repository configured");
            return null;
        }

        try {
            // only now, knowing the session (tied to particular local repository) we can
            // preprocess list of remote repositories, applying mirror, proxy and auth configuration
            // this was done manually in Pax URL Aether 2.6.x, where assignProxyAndMirrors() was doing
            // complex mirror/proxy/auth processing and also checking "effective" checsum/update policies
            // inside a mirror when it was used to mirror more repositories.
            // now everything is done with single call
            List<RemoteRepository> configuredRepositories
                    = assignMirrorsAndProxies(session, remoteRepositories);

            artifact = resolveLatestVersionRange(session, configuredRepositories, artifact);
            return m_repoSystem
                    .resolveArtifact(session, new ArtifactRequest(artifact, configuredRepositories, null))
                    .getArtifact().getFile();
        } catch (ArtifactResolutionException e) {
            // we know there's one ArtifactResult, because there was one ArtifactRequest
            ArtifactResolutionException original = new ArtifactResolutionException(e.getResults(),
                    "Error resolving artifact " + artifact.toString(), null);

            throw configureIOException(original, e, e.getResult().getExceptions());
        } catch (VersionRangeResolutionException e) {
            // we know there's one ArtifactResult, because there was one ArtifactRequest
            VersionRangeResolutionException original = new VersionRangeResolutionException(e.getResult(),
                    "Error resolving artifact " + artifact.toString(), null);

            throw configureIOException(original, e, e.getResult().getExceptions());
        } finally {
            releaseSession(session);
        }
    }

    @Override
    public File resolveMetadata(String groupId, String artifactId, String type, String version) throws IOException {
        return resolveMetadata(groupId, artifactId, type, version, null);
    }

    @Override
    public File resolveMetadata(String groupId, String artifactId, String type, String version, Exception previousException) throws IOException {
        RepositorySystemSession session = findOrCreateSession(null);
        if (session == null) {
            LOG.warn("Can't resolve metadata without configured local repository");
            return null;
        }

        try {
            Metadata metadata = new DefaultMetadata(groupId, artifactId, version,
                    type, Metadata.Nature.RELEASE_OR_SNAPSHOT);
            List<MetadataRequest> requests = new ArrayList<>();

            // configured repositories
            List<RemoteRepository> remoteRepositories = selectRemoteRepositories(null);
            // repositories processed (mirrors, proxies, auth)
            List<RemoteRepository> configuredRepositories
                    = assignMirrorsAndProxies(session, remoteRepositories);

            for (RemoteRepository repository : configuredRepositories) {
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
            List<MetadataResult> results = m_repoSystem.resolveMetadata(session, requests);
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
                mr.getVersioning().getVersions().sort(VERSION_COMPARATOR);
                mr.getVersioning().getSnapshotVersions().sort(SNAPSHOT_VERSION_COMPARATOR);
                File tmpFile = Files.createTempFile("mvn-", ".tmp").toFile();
                try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    new MetadataXpp3Writer().write(fos, mr);
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

    private String latestTimestamp(String t1, String t2) {
        if (t1 == null) {
            return t2;
        } else if (t2 == null) {
            return t1;
        } else {
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

    // ---- upload methods

    @Override
    public void upload(String groupId, String artifactId, String classifier, String extension, String version,
            File file) throws IOException {
        RepositorySystemSession session = findOrCreateSession(null);
        if (session == null) {
            return;
        }
        try {
            Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version,
                    null, file);
            InstallRequest request = new InstallRequest();
            request.addArtifact(artifact);
            m_repoSystem.install(session, request);
        } catch (Exception e) {
            throw new IOException("Unable to install artifact", e);
        } finally {
            releaseSession(session);
        }
    }

    @Override
    public void uploadMetadata(String groupId, String artifactId, String type, String version,
            File artifact) throws IOException {
        RepositorySystemSession session = findOrCreateSession(null);
        if (session == null) {
            return;
        }
        try {
            Metadata metadata = new DefaultMetadata(groupId, artifactId, version,
                    type, Metadata.Nature.RELEASE_OR_SNAPSHOT, artifact);
            InstallRequest request = new InstallRequest();
            request.addMetadata(metadata);
            m_repoSystem.install(session, request);
        } catch (Exception e) {
            throw new IOException("Unable to install metadata", e);
        } finally {
            releaseSession(session);
        }
    }

    // ---- resolution helper methods

    @Override
    @SuppressWarnings({ "ReassignedVariable", "DataFlowIssue" })
    public RetryChance isRetryableException(Exception exception) {
        RetryChance retry = RetryChance.NEVER;

        RepositoryException aetherException = findAetherException(exception);

        if (aetherException instanceof ArtifactResolutionException) {
            // aggregate case - exception that contains exceptions - usually per repository
            ArtifactResolutionException resolutionException = (ArtifactResolutionException) aetherException;
            if (resolutionException.getResult() != null) {
                for (Exception ex : resolutionException.getResult().getExceptions()) {
                    RetryChance singleRetry = isRetryableException(ex);
                    if (retry.chance() < singleRetry.chance()) {
                        retry = singleRetry;
                    }
                }
            }
        } else if (aetherException != null) {
            // single exception case

            if (aetherException instanceof ArtifactNotFoundException) {
                // very little chance we'll find the artifact next time
                retry = RetryChance.NEVER;
            } else if (aetherException instanceof MetadataNotFoundException) {
                retry = RetryChance.NEVER;
            } else if (aetherException instanceof ArtifactTransferException
                    || aetherException instanceof MetadataTransferException) {
                // we could try again
                Throwable root = rootException(aetherException);
                if (root instanceof SocketTimeoutException) {
                    // we could try again - but without assuming we'll succeed eventually
                    retry = RetryChance.LOW;
                } else if (root instanceof ConnectException) {
                    // "connection refused" - not retryable
                    retry = RetryChance.NEVER;
                } else if (root instanceof NoRouteToHostException) {
                    // not retryable
                    retry = RetryChance.NEVER;
                }
            } else {
                // general aether exception - let's fallback to NEVER, as retryable cases should be
                // handled explicitly
                retry = RetryChance.NEVER;
            }
        } else {
            // we don't know about non-aether exceptions, so let's allow
            retry = RetryChance.UNKNOWN;
        }

        return retry;
    }

    /**
     * Find top-most Aether exception
     * @param e
     * @return
     */
    protected RepositoryException findAetherException(Exception e) {
        Throwable ex = e;
        while (ex != null && !(ex instanceof RepositoryException)) {
            ex = ex.getCause();
        }
        return ex == null ? null : (RepositoryException) ex;
    }

    /**
     * Find root exception
     * @param ex
     * @return
     */
    protected Throwable rootException(Exception ex) {
        Throwable root = ex;
        while (true) {
            if (root.getCause() != null) {
                root = root.getCause();
            } else {
                break;
            }
        }
        return root;
    }

    /**
     * Take original Maven exception's message and stack trace without suppressed exceptions. Suppressed
     * exceptions will be taken from {@code ArtifactResult} or {@link VersionRangeResult}
     *
     * @param newMavenException exception with reconfigured suppressed exceptions
     * @param cause original Maven exception
     * @param resultExceptions
     * @return
     */
    private IOException configureIOException(Exception newMavenException, Exception cause, List<Exception> resultExceptions) {
        newMavenException.setStackTrace(cause.getStackTrace());

        List<String> messages = new ArrayList<>(resultExceptions.size());
        List<Exception> suppressed = new ArrayList<>();
        for (Exception ex : resultExceptions) {
            messages.add(ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
            suppressed.add(ex);
        }
        IOException exception = new IOException(newMavenException.getMessage() + ": " + messages, newMavenException);
        for (Exception ex : suppressed) {
            exception.addSuppressed(ex);
        }
        LOG.warn(exception.getMessage(), exception);

        return exception;
    }

    /**
     * Tries to resolve versions = LATEST using an open range version query. If it succeeds, version
     * of artifact is set to the highest available version.
     *
     * @param session
     * @param artifact
     * @return an artifact with version set properly (highest if available)
     * @throws {@link VersionRangeResolutionException}
     */
    private Artifact resolveLatestVersionRange(RepositorySystemSession session,
            List<RemoteRepository> repositories, Artifact artifact)
            throws VersionRangeResolutionException {

        VersionRangeResult versionResult = m_repoSystem.resolveVersionRange(session,
                new VersionRangeRequest(artifact, repositories, null));
        if (versionResult != null) {
            Version v = versionResult.getHighestVersion();
            if (v != null) {
                artifact = artifact.setVersion(v.toString());
            } else {
                throw new VersionRangeResolutionException(versionResult, "No highest version found for " + artifact);
            }
        }
        return artifact;
    }

    // ---- methods that prepare lists of repositories to use

    /**
     * Prepare list of <em>local repositories</em> which serve the purpose of <em>default repositories</em>.
     * These are Pax URL specific read-only repositories used to get locally available artifacts without
     * remote resolution. The best example is Karaf's {@code system/} directory.
     *
     * @return
     */
    List<LocalRepositoryWithConfig> selectDefaultRepositories() {
        List<LocalRepositoryWithConfig> list = new ArrayList<>();
        List<MavenRepositoryURL> urls = Collections.emptyList();
        try {
            urls = m_config.getDefaultRepositories();
        } catch (MalformedURLException exc) {
            LOG.error("Invalid default repository URLs", exc);
        }

        for (MavenRepositoryURL r : urls) {
            if (r.getFile() == null) {
                LOG.warn("Invalid default repository {}. Only local directories are supported.", r);
                continue;
            }
            if (r.isMulti()) {
                selectDefaultRepositories(list, r, r.getFile());
            } else {
                selectDefaultRepository(list, r);
            }
        }

        return list;
    }

    /**
     * Adds each subdirectory of given {@code parentDir} as {@link LocalRepository} with proper configuration.
     *
     * @param list
     * @param parentRepo
     * @param parentDir
     */
    private void selectDefaultRepositories(List<LocalRepositoryWithConfig> list, MavenRepositoryURL parentRepo, File parentDir) {
        if (!parentDir.isDirectory()) {
            LOG.warn("Repository marked with @multi does not resolve to a directory: {}", parentDir);
            return;
        }

        for (File repo : getSortedChildDirectories(parentDir)) {
            MavenRepositoryURL child = new MavenRepositoryURL(parentRepo, repo);
            String repoURI = child.getURI().toString() + "@" + child.getId();
            LOG.debug("Adding default repository from multi dir: {}", repoURI);
            selectDefaultRepository(list, child);
        }
    }

    /**
     * One of 2 places where an instance of {@link LocalRepository} is created. This is where
     * {@link LocalRepository} is in the role of <em>default repository</em> - a read-only location
     * for artifacts (no remote cache role).
     *
     * @param list
     * @param repo
     */
    private void selectDefaultRepository(List<LocalRepositoryWithConfig> list, MavenRepositoryURL repo) {
        if (repo.getFile() != null) {
            LocalRepository local = new LocalRepository(repo.getFile(), ENHANCED_REPOSITORY_TYPE);
            list.add(new LocalRepositoryWithConfig(local, repo));
        }
    }

    /**
     * <p>Prepare list of <em>remote repositories</em> which are searched for an artifact (or metadata) which is not
     * available (cached) in any <em>default repository</em>. If artifact is resolved and downloaded it is stored
     * (cached) in single {@link LocalRepository} (from current {@link RepositorySystemSession session}.</p>
     *
     * <p>Returned list of remote repositories is not yet processed (mirrors, proxies, auth).</p>
     *
     * @param extraRepository
     * @return
     */
    List<RemoteRepository> selectRemoteRepositories(MavenRepositoryURL extraRepository) {
        List<RemoteRepository> list = new ArrayList<>();
        List<MavenRepositoryURL> urls = Collections.emptyList();
        try {
            urls = m_config.getRepositories();
        } catch (MalformedURLException exc) {
            LOG.error("Invalid remote repository URLs", exc);
        }

        List<MavenRepositoryURL> toCheck = new ArrayList<>(urls);
        if (extraRepository != null) {
            toCheck.add(extraRepository);
        }

        for (MavenRepositoryURL r : toCheck) {
            if (r.isSplit()) {
                LOG.warn("Remote repository {} is configured with @split option. " +
                        "Split repositories can only be default or local. Ignoring.", r);
                continue;
            }
            if (r.isMulti()) {
                if (r.getFile() == null) {
                    LOG.warn("Remote repository {} is marked as @multi repository, but is not using file: protocol", r);
                } else {
                    selectRemoteRepositories(list, r.getFile());
                }
            } else {
                selectRemoteRepository(list, r);
            }
        }

        return list;
    }

    /**
     * Adds each subdirectory of given {@code parentDir} as {@link RemoteRepository} with proper configuration.
     *
     * @param list
     * @param parentDir
     */
    private void selectRemoteRepositories(List<RemoteRepository> list, File parentDir) {
        if (!parentDir.isDirectory()) {
            LOG.debug("Repository marked with @multi does not resolve to a directory: {}", parentDir);
            return;
        }

        for (File repo : getSortedChildDirectories(parentDir)) {
            try {
                String repoURI = repo.toURI().toString() + "@id=" + repo.getName();
                LOG.debug("Adding remote repository from multi dir: {}", repoURI);
                selectRemoteRepository(list, new MavenRepositoryURL(repoURI));
            } catch (MalformedURLException e) {
                LOG.error("Error resolving remote repository url of a @multi directory {}", repo.toURI());
            }
        }
    }

    /**
     * A {@link RemoteRepository} is added to the passed list. This repository will be used for remote Maven resolution
     * (even if file:-based).
     *
     * @param list
     * @param repo
     */
    private void selectRemoteRepository(List<RemoteRepository> list, MavenRepositoryURL repo) {
        if (repo.getId() == null) {
            throw new IllegalArgumentException("RepositoryURL doesn't have ID configured: " + repo);
        }

        RemoteRepository.Builder builder = new RemoteRepository.Builder(repo.getId(), ENHANCED_REPOSITORY_TYPE, repo.getURI().toString());

        // In Pax URL before version 3, we were using "global" update/checksum policies in RepositorySystemSession
        // In Pax URL 3 we'll use global values only to configure per-repository (not per-session) values if
        // repository doesn't specify its own value
        String releasesUpdatePolicy = repo.getReleasesUpdatePolicy();
        if (releasesUpdatePolicy == null || releasesUpdatePolicy.isEmpty()) {
            releasesUpdatePolicy = m_config.getGlobalUpdatePolicy();
            if (releasesUpdatePolicy == null || releasesUpdatePolicy.isEmpty()) {
                // by default, never update from releases repository
                releasesUpdatePolicy = UPDATE_POLICY_NEVER;
            }
        }
        String releasesChecksumPolicy = repo.getReleasesChecksumPolicy();
        if (releasesChecksumPolicy == null || releasesChecksumPolicy.isEmpty()) {
            releasesChecksumPolicy = m_config.getGlobalChecksumPolicy();
            if (releasesChecksumPolicy == null || releasesChecksumPolicy.isEmpty()) {
                releasesChecksumPolicy = CHECKSUM_POLICY_WARN;
            }
        }
        RepositoryPolicy releasePolicy = new RepositoryPolicy(repo.isReleasesEnabled(),
                releasesUpdatePolicy, releasesChecksumPolicy);

        String snapshotsUpdatePolicy = repo.getSnapshotsUpdatePolicy();
        if (snapshotsUpdatePolicy == null || snapshotsUpdatePolicy.isEmpty()) {
            snapshotsUpdatePolicy = m_config.getGlobalUpdatePolicy();
            if (snapshotsUpdatePolicy == null || snapshotsUpdatePolicy.isEmpty()) {
                // by default update "daily" for snapshots
                snapshotsUpdatePolicy = UPDATE_POLICY_DAILY;
            }
        }
        String snapshotsChecksumPolicy = repo.getSnapshotsChecksumPolicy();
        if (snapshotsChecksumPolicy == null || snapshotsChecksumPolicy.isEmpty()) {
            snapshotsChecksumPolicy = m_config.getGlobalChecksumPolicy();
            if (snapshotsChecksumPolicy == null || snapshotsChecksumPolicy.isEmpty()) {
                snapshotsChecksumPolicy = CHECKSUM_POLICY_WARN;
            }
        }
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy(repo.isSnapshotsEnabled(),
                snapshotsUpdatePolicy, snapshotsChecksumPolicy);

        builder.setReleasePolicy(releasePolicy);
        builder.setSnapshotPolicy(snapshotPolicy);

        // when MavenRepositoryURL has username/password configured, we can attach an Authentication
        // object to the builder - it'll be handled by
        // org.eclipse.aether.util.repository.ConservativeAuthenticationSelector
        if (repo.getUsername() != null && repo.getPassword() != null) {
            builder.setAuthentication(new AuthenticationBuilder()
                    .addUsername(repo.getUsername()).addPassword(repo.getPassword())
                    .build());
        }

        list.add(builder.build());
    }

    /**
     * <p>For the given parent, we find all child files, and then
     * sort those files by their name (not absolute path).</p>
     *
     * <p>The sorted list is returned, or an empty list if listFiles returns null.</p>
     *
     * @param parent A non-null parent File for which you want to get the sorted list of child directories.
     * @return The alphabetically sorted list of files, or an empty list if parent.listFiles() returns null.
     */
    private static File[] getSortedChildDirectories(File parent) {
        File[] files = parent.listFiles(File::isDirectory);
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, new Comparator<>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return files;
    }

    /**
     * <p>Pax URL 2.6 manually checked mirros, proxies and authentication configuration to replace
     * list of target {@link RemoteRepository repositories} with mirrored/proxied ones.</p>
     *
     * <p>Here we'll use {@link RepositorySystem#newResolutionRepositories}. If passes session is null, a
     * static session is used that satisfies requirements of underlying resolver mechanisms (like getting
     * global update/checksum policies).</p>
     *
     * @param session
     * @param repositories
     * @return
     */
    public List<RemoteRepository> assignMirrorsAndProxies(RepositorySystemSession session, List<RemoteRepository> repositories) {
        RepositorySystemSession s = session != null ? session : m_defaultSession;
        return m_repoSystem.newResolutionRepositories(s, repositories);
    }

    // ---- methods that produce Maven Resolver objects (system and session)

    /**
     * <p>Create and return an instance of {@link RepositorySystem} for all resolution operations.</p>
     *
     * <p>Before Maven Resolver 1.9 (so for example when using previous Eclipse Aether Resolver),
     * a <em>service locator</em> pattern was used where all Maven/Resolver related components were configured in
     * the locator as services. With Maven Resolver 1.9, this locator pattern was deprecated in favor of full DI
     * solution and this was the approach taken in Camel (see <a href="https://issues.apache.org/jira/browse/CAMEL-18555">CAMEL-18555</a>
     * and <a href="https://issues.apache.org/jira/browse/MRESOLVER-157">MRESOLVER-157</a>).</p>
     *
     * <p>Finally a <em>supplier</em> pattern emerged with <a href="https://issues.apache.org/jira/browse/MRESOLVER-387">MRESOLVER-387</a>
     * and we use it here overriding tiny parts of functionality.</p>
     * @return
     */
    private RepositorySystem newRepositorySystem() {
        return new PaxRepositorySystemSupplier().get();
    }

    /**
     * Returns a {@link RepositorySystemSession} which contains {@link LocalRepository} specific
     * configuration. Existing session is taken from cache (deque) and created if there's no session
     * for given repository yet.
     *
     * @param repo
     * @return
     */
    private RepositorySystemSession findOrCreateSession(LocalRepositoryWithConfig repo) {
        if (repo == null) {
            if (m_localRepository == null) {
                // user may explicitly (somehow) set this to null. It means no remote resolution can be performed
                return null;
            }

            // fall back to "global" local repository
            repo = new LocalRepositoryWithConfig(m_localRepository, m_config.getLocalMavenRepositoryURL());
        }

        RepositorySystemSession session = null;
        if (!m_shutdown.get()) {
            // when the resolver is shut down, let other thread continue with non-cached session - it may
            // fail later, but the session cache will be empty
            Deque<RepositorySystemSession> deque = sessions.get(repo.repository);
            if (deque != null) {
                session = deque.pollFirst();
            }
        }
        if (session == null) {
            session = newRepositorySystemSession(repo);
        }
        return session;
    }

    /**
     * Release a session for given local repository, so it may be used during next resolution.
     *
     * @param session
     */
    private void releaseSession(RepositorySystemSession session) {
        if (!m_shutdown.get()) {
            LocalRepository repo = session.getLocalRepository();
            Deque<RepositorySystemSession> deque = sessions.get(repo);
            if (deque == null) {
                sessions.putIfAbsent(repo, new ConcurrentLinkedDeque<>());
                deque = sessions.get(repo);
            }
            session.getData().set(SESSION_CHECKS, null);
            deque.add(session);
        }
    }

    /**
     * <p>Create and return an instance of {@link RepositorySystemSession} for all resolution operations.</p>
     *
     * @return
     */
    private RepositorySystemSession newRepositorySystemSession(LocalRepositoryWithConfig repo) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        // before calling system.newLocalRepositoryManager() we have to set session config properties
        // because these are required by DefaultLocalPathPrefixComposerFactory.createComposer()
        // for split repositories

        MavenRepositoryURL repoURL = repo.repositoryURL;

        // the property names are protected in
        // org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactorySupport ...
        session.setConfigProperty("aether.enhancedLocalRepository.split", repoURL.isSplit());
        session.setConfigProperty("aether.enhancedLocalRepository.splitLocal", repoURL.isSplitLocal());
        session.setConfigProperty("aether.enhancedLocalRepository.splitRemote", repoURL.isSplitRemote());
        session.setConfigProperty("aether.enhancedLocalRepository.splitRemoteRepository", repoURL.isSplitRemoteRepository());
        session.setConfigProperty("aether.enhancedLocalRepository.splitRemoteRepositoryLast", repoURL.isSplitRemoteRepositoryLast());
        session.setConfigProperty("aether.enhancedLocalRepository.localPrefix", repoURL.getSplitLocalPrefix());
        session.setConfigProperty("aether.enhancedLocalRepository.remotePrefix", repoURL.getSplitRemotePrefix());
        session.setConfigProperty("aether.enhancedLocalRepository.releasesPrefix", repoURL.getSplitReleasesPrefix());
        session.setConfigProperty("aether.enhancedLocalRepository.snapshotsPrefix", repoURL.getSplitSnapshotsPrefix());

        session.setLocalRepositoryManager(m_repoSystem.newLocalRepositoryManager(session, repo.repository));

        session.setMirrorSelector(m_mirrorSelector);
        session.setProxySelector(m_proxySelector);
        session.setAuthenticationSelector(m_authenticationSelector);

        // some extra configuration
        session.setOffline(m_config.isOffline());
        session.setConfigProperty(ConfigurationProperties.INTERACTIVE, false);
        session.setConfigProperty(ConfigurationProperties.HTTP_PREEMPTIVE_AUTH, true);

        session.setConfigProperty("aether.connector.basic.threads", 3);
        session.setConfigProperty("aether.dependencyCollector.impl", "df"); // or "bf"
        session.setConfigProperty(ConfigurationProperties.USER_AGENT, "Pax URL");

        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(false, false));

        // PAXURL-322
        boolean updateReleases = m_config.getProperty(ServiceConstants.PROPERTY_UPDATE_RELEASES, false, Boolean.class);
        // because we should access it later using org.eclipse.aether.RepositorySystemSession.getConfigProperties()
        session.setConfigProperty(PaxLocalRepositoryManager.PROPERTY_UPDATE_RELEASES, updateReleases);

        // never call setUpdatePolicy() or setChecksumPolicy() on session, as this will override
        // values specified for each individual RemoteRepository
        session.setUpdatePolicy(null);
        session.setChecksumPolicy(null);

        // https://maven.apache.org/resolver/configuration.html lists all the supported properties
        // some of them are marked as "Supports Repo ID Suffix", which means the keys can be suffixed with "." + serverId
        // which gives us per-repository configuration

        @SuppressWarnings("deprecation")
        int defaultTimeout = m_config.getTimeout();
        int connectionTimeout = m_config.getProperty(ServiceConstants.PROPERTY_SOCKET_CONNECTION_TIMEOUT, defaultTimeout, Integer.class);
        int readTimeout = m_config.getProperty(ServiceConstants.PROPERTY_SOCKET_SO_TIMEOUT, defaultTimeout, Integer.class);

        // org.apache.http.client.config.RequestConfig.Builder.setConnectTimeout()
        // org.apache.http.client.config.RequestConfig.Builder.setConnectionRequestTimeout()
        session.setConfigProperty(ConfigurationProperties.CONNECT_TIMEOUT, connectionTimeout);
        // org.apache.http.config.SocketConfig.Builder.setSoTimeout()
        // org.apache.http.client.config.RequestConfig.Builder.setSocketTimeout()
        session.setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, readTimeout);

        // configuration related to maven-resolver-transport-http. Pax URL 2.6 configured its own
        // instance of org.apache.http.impl.client.CloseableHttpClient and put it into manual wagon
        // in Pax URL 3 we can use properties from https://maven.apache.org/resolver/configuration.html
        // see how the client is configured in org.eclipse.aether.transport.http.HttpTransporter constructor

        // this option is for org.apache.http.impl.client.HttpClientBuilder.useSystemProperties()
        // which decides about usage of https.cipherSuites, https.protocols, http.keepAlive,
        // http.maxConnections, http.agent, http.proxyHost and related system properties
        // if this mode is on, javax.net.ssl.SSLSocketFactory.getDefault() is used, when it's off (default)
        // org.apache.http.ssl.SSLContexts.createDefault() is called
        // see https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html
        // also for proxies, it chooses between:
        //  - org.apache.http.impl.conn.DefaultProxyRoutePlanner (when off)
        //  - org.apache.http.impl.conn.SystemDefaultRoutePlanner (when on, delegates to java.net.ProxySelector.getDefault())
        session.setConfigProperty("aether.connector.http.useSystemProperties", m_useSystemProperties);

        // configuration used by Pax URL 2.6, but not available in Pax URL 3 with maven-resolver-transport-http
        // where HttpClient is configured using https://maven.apache.org/resolver/configuration.html:
        //  - HttpClientBuilder.disableConnectionState()

        // HttpClient connection manager
        // see org.eclipse.aether.transport.http.GlobalState.newConnectionManager()
        // "default":
        //  - javax.net.ssl.SSLSocketFactory.getDefault()
        //  - org.apache.http.conn.ssl.SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        // "insecure":
        //  - org.apache.http.conn.ssl.TrustAllStrategy
        //  - org.apache.http.conn.ssl.NoopHostnameVerifier
        boolean secure = m_config.getCertificateCheck();
        session.setConfigProperty(ConfigurationProperties.HTTPS_SECURITY_MODE, secure
                ? ConfigurationProperties.HTTPS_SECURITY_MODE_DEFAULT
                : ConfigurationProperties.HTTPS_SECURITY_MODE_INSECURE);

        // HttpClient retry handler (default: 3)
        int resolverRetryCount = m_config.getProperty(ConfigurationProperties.HTTP_RETRY_HANDLER_COUNT, ConfigurationProperties.DEFAULT_HTTP_RETRY_HANDLER_COUNT, Integer.class);
        int retryCount = m_config.getProperty(ServiceConstants.PROPERTY_CONNECTION_RETRY_COUNT, resolverRetryCount, Integer.class);
        session.setConfigProperty(ConfigurationProperties.HTTP_RETRY_HANDLER_COUNT, resolverRetryCount);

        // iterate over available servers (mind that these are before proxying/mirroring, because
        // org.eclipse.aether.RepositorySystem.newResolutionRepositories() needs a session to work) and collect
        // available configuration (timeouts and headers) from setings.xml
        // see how it's done in eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport.newRepositorySession()

        for (Server s : m_settings.getServers()) {
            String sid = s.getId();
            if (s.getFilePermissions() != null) {
                session.setConfigProperty("aether.connector.perms.fileMode." + sid, s.getFilePermissions());
            }
            if (s.getDirectoryPermissions() != null) {
                session.setConfigProperty("aether.connector.perms.dirMode." + sid, s.getFilePermissions());
            }

            if (s.getConfiguration() == null) {
                continue;
            }

            Xpp3Dom config = (Xpp3Dom) s.getConfiguration();

            // https://maven.apache.org/guides/mini/guide-http-settings.html shows old wagon config for Maven 3.8
            // https://maven.apache.org/guides/mini/guide-resolver-transport.html shows new config for Maven 3.9

            // the old wagon config gives us a lot of configuration, including options like preemptive auth
            // using <httpConfiguration>/<get>/<params>/<property>/<name> = http.authentication.preemptive
            // with new config, only headers and timeouts are specified in XML and remaining parameters
            // are configured using Aether configuration options from https://maven.apache.org/resolver/configuration.html

            int serverConnectTimeout = connectionTimeout;
            int serverRequestTimeout = readTimeout;

            // timeouts, the new way
            // <server>
            //   <id>my-server</id>
            //   <configuration>
            //     <connectTimeout>5000</connectTimeout>
            //     <requestTimeout>5000</requestTimeout>
            //   </configuration>
            // </server>
            Xpp3Dom connectTimeoutValue = config.getChild("connectTimeout");
            if (connectTimeoutValue != null) {
                try {
                    serverConnectTimeout = Integer.parseInt(connectTimeoutValue.getValue());
                } catch (NumberFormatException ignored) {
                }
            }
            Xpp3Dom requestTimeoutValue = config.getChild("requestTimeout");
            if (requestTimeoutValue != null) {
                try {
                    serverRequestTimeout = Integer.parseInt(requestTimeoutValue.getValue());
                } catch (NumberFormatException ignored) {
                }
            }
            session.setConfigProperty(ConfigurationProperties.CONNECT_TIMEOUT + "." + sid, serverConnectTimeout);
            session.setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT + "." + sid, serverRequestTimeout);

            // headers. the new way
            // <server>
            //   <id>my-server</id>
            //   <configuration>
            //     <httpHeaders>
            //       <property>
            //         <name>Foo</name>
            //         <value>Bar</value>
            //       </property>
            //     </httpHeaders>
            //   </configuration>
            // </server>
            Map<String, String> headers = new HashMap<>();
            Xpp3Dom httpHeaders = config.getChild("httpHeaders");
            if (httpHeaders != null) {
                for (Xpp3Dom httpHeader : httpHeaders.getChildren("property")) {
                    Xpp3Dom name = httpHeader.getChild("name");
                    String headerName = name == null ? null : name.getValue();
                    Xpp3Dom value = httpHeader.getChild("value");
                    String headerValue = value == null ? null : value.getValue();
                    if (name != null && value != null) {
                        headers.put(headerName, headerValue);
                    }
                }
            }
            if (!headers.isEmpty()) {
                session.setConfigProperty(ConfigurationProperties.HTTP_HEADERS + "." + sid, headers);
            }

            // timeouts and headers, the old way
            // <server>
            //   <id>my-server</id>
            //   <configuration>
            //     <httpConfiguration>
            //       <all>
            //         <connectionTimeout>5000</connectionTimeout>
            //         <readTimeout>10000</readTimeout>
            //         <useDefaultHeaders>false</useDefaultHeaders>
            //         <headers>
            //           <property>
            //             <name>Cache-control</name>
            //             <value>no-cache</value>
            //           </property>
            //           ...
            //         </headers>
            //       </all>
            //     </httpConfiguration>
            //   </configuration>
            // </server>
            Xpp3Dom httpConfiguration = config.getChild("httpConfiguration");
            if (httpConfiguration != null) {
                LOG.warn("Old <configuration>/<httpConfiguration> detected." +
                        " Please check https://maven.apache.org/guides/mini/guide-resolver-transport.html" +
                        " for new configuration style.");
            }
        }

        return session;
    }

    private RepositorySystemSession newDefaultRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setLocalRepositoryManager(m_repoSystem.newLocalRepositoryManager(session, m_localRepository));

        session.setMirrorSelector(m_mirrorSelector);
        session.setProxySelector(m_proxySelector);
        session.setAuthenticationSelector(m_authenticationSelector);

        // some extra configuration
        session.setOffline(m_config.isOffline());
        session.setConfigProperty(ConfigurationProperties.INTERACTIVE, false);

        session.setUpdatePolicy(null);
        session.setChecksumPolicy(null);

        return session;
    }

    /**
     * Combination of {@link LocalRepository} and {@link MavenRepositoryURL}
     */
    public static class LocalRepositoryWithConfig {
        final LocalRepository repository;
        final MavenRepositoryURL repositoryURL;

        private LocalRepositoryWithConfig(LocalRepository repository, MavenRepositoryURL repositoryURL) {
            this.repository = repository;
            this.repositoryURL = repositoryURL;
        }
    }

    private static final Comparator<String> VERSION_COMPARATOR = new Comparator<>() {
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

    private static final Comparator<SnapshotVersion> SNAPSHOT_VERSION_COMPARATOR = new Comparator<>() {
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

}
