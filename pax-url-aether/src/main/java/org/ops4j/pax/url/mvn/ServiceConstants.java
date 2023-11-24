/*
 * Copyright 2011 Toni Menzel.
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
package org.ops4j.pax.url.mvn;

/**
 * An enumeration of constants related to Maven handler. When using ConfigurationAdmin, it is recommended to
 * prepend the property names with {@code <PID>.} prefix.
 *
 * @author Toni Menzel
 * @author Guillaume Nodet
 * @since September 10, 2010
 */
public interface ServiceConstants
{
    /**
     * Service PID used for configuration.
     */
    String PID = "org.ops4j.pax.url.mvn";
    
    /**
     * The protocol name.
     */
    String PROTOCOL = "mvn";

    /**
     * <p>If present, entire configuration is rejected. This allows Pax URL Aether to wait for a proper Configuration
     * Admin config without this option specified (for timing reasons).</p>
     *
     * <p><strong>Warning: use only in framework properties.</strong></p>
     */
    String REQUIRE_CONFIG_ADMIN_CONFIG = "requireConfigAdminConfig";

    /**
     * If this environmental variable is specified, the value will be used as global Maven mirror
     * (for all repositories - {@code <mirrorOf>*</mirrorOf>}), replacing all mirrors configured in
     * external settings file. Mirror value can be specified using two forms:<ul>
     *     <li>{@code <mirror url>}</li>
     *     <li>{@code <mirror id>::<mirror url>}</li>
     * </ul>
     */
    String ENV_MAVEN_MIRROR_URL = "MAVEN_MIRROR_URL";

    /**
     * If this system property is specified, the value will be used as global Maven mirror
     * (for all repositories - {@code <mirrorOf>*</mirrorOf>}), replacing all mirrors configured in
     * external settings file. Mirror value can be specified using two forms:<ul>
     *     <li>{@code <mirror url>}</li>
     *     <li>{@code <mirror id>::<mirror url>}</li>
     * </ul>
     */
    String SYS_MAVEN_MIRROR_URL = "mavenMirrorUrl";

    // ---- Property names to be used in a dictionary for org.ops4j.pax.url.mvn.internal.config.MavenConfiguration

    /**
     * Should Maven Resolver work in offline mode? (no remote repository access).
     */
    String PROPERTY_OFFLINE = "offline";

    /**
     * <p>File name (or URL, but only with {@code file:} scheme) for external {@code settings.xml} file.
     * Can be used to specify non-default location (defaults to {@code ~/.m2/settings.xml},
     * {@code ${maven.home}/conf/settings.xml} and {@code $MAVEN_HOME/conf/settings.xml}, in such order).</p>
     *
     * <p>Can be set to values like {@code false}, {@code none} or {@code no} to skip searching for default
     * location.</p>
     */
    String PROPERTY_SETTINGS_FILE = "settings";

    /**
     * <p>File name (or URL, but only with {@code file:} scheme) for external {@code settings-security.xml} file.
     * Can be used to specify non-default location (defaults to {@code ~/.m2/settings-security.xml}.</p>
     *
     * <p>Can be set to values like {@code false}, {@code none} or {@code no} to skip searching for default
     * location.</p>
     */
    String PROPERTY_SETTINGS_SECURITY_FILE = "security";

    /**
     * <p>Global update policy property name (values: {@code never}, {@code daily}, {@code always} or
     * {@code interval:N} (minutes)). Used when repository-specific policy isn't configured.</p>
     *
     * <p>Provides <a href="http://maven.apache.org/settings.html">repository update policy</a> which
     * will be applied to all configured repositories.</p>
     */
    String PROPERTY_GLOBAL_UPDATE_POLICY = "globalUpdatePolicy";

    /**
     * <p>Global checksum policy property name (values: {@code ignore}, {@code warn}, {@code fail}).
     * Used when repository-specific policy isn't configured.</p>
     *
     * <p>Provides <a href="http://maven.apache.org/settings.html">repository checksum policy</a> which
     * will be applied to all configured repositories.</p>
     */
    String PROPERTY_GLOBAL_CHECKSUM_POLICY = "globalChecksumPolicy";

    /**
     * <p>Property for configuration of non-canonical Maven behavior. If set to <code>true</code>,
     * {@link MavenResolver} will use Aether policies to determine whether already available non-SNAPSHOT
     * artifact should be redownloaded.</p>
     * <p>Default value is <code>false</code>.</p>
     */
    String PROPERTY_UPDATE_RELEASES = "updateReleases";

    /**
     * <p>Directory name (or URL, but only with {@code file:} scheme) for Maven local repository used. Can be used
     * to specify non-default location (defaults to {@code <settings>/<localRepository>} from found
     * {@code settings.xml}, {@code maven.repo.local} system property or eventually, {@code ~/.m2/repository}).</p>
     */
    String PROPERTY_LOCAL_REPOSITORY = "localRepository";

    /**
     * Property to store {@link org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL} representing
     * configured local repository.
     */
    String PROPERTY_LOCAL_REPOSITORY_URL = "localRepositoryUrl";

    /**
     * Option to enable split local repository for cached (remote) and local (installed) artifacts.
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT = "split";

    /**
     * Subdirectory for split installed (local) artifacts in local repository. Defaults to "installed".
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_LOCAL_PREFIX = "splitLocalPrefix";

    /**
     * Subdirectory for split cached (remote) artifacts in local repository. Defaults to "cached".
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_REMOTE_PREFIX = "splitRemotePrefix";

    /**
     * Option to enable further splitting of locally installed subdirectory within a local repository to
     * release and snapshot versions.
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_LOCAL = "splitLocal";

    /**
     * Option to enable further splitting of remote cached subdirectory within a local repository to
     * release and snapshot versions.
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_REMOTE = "splitRemote";

    /**
     * Subdirectory for split installed and cached non-snapshot artifacts in local repository. Defaults to "releases".
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_LOCAL_RELEASES = "splitReleasesPrefix";

    /**
     * Subdirectory for split installed and cached snapshot artifacts in local repository. Defaults to "snapshots".
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_LOCAL_SNAPSHOTS = "splitSnapshotsPrefix";

    /**
     * Option to enable further splitting of remote cached subdirectory of local repository by origin remote
     * repository ID.
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_REMOTE_REPOSITORY = "splitRemoteRepository";

    /**
     * If split by remote repository id is effective, whether to use remote repository ID after (when {@code true})
     * of before (when {@code false}) the split segment for releases/snapshots.
     */
    String PROPERTY_LOCAL_REPOSITORY_SPLIT_REMOTE_REPOSITORY_LAST = "splitRemoteRepositoryLast";

    /**
     * DefaultRepositories configuration property name. Specifies comma-separated list of default repository URLs in
     * the format of {@link org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL}.
     */
    String PROPERTY_DEFAULT_REPOSITORIES = "defaultRepositories";

    /**
     * <p>Repositories configuration property name. Specifies comma-separated list of remote repository URLs in
     * the format of {@link org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL}.</p>
     *
     * <p>If the property value starts with {@code +}, the repository list is appended with repositories
     * found in configured {@code settings.xml} file. If the value is not empty, but doesn't start with plus sign,
     * the repositories from configured {@code settings.xml} are <strong>NOT</strong> used.</p>
     */
    String PROPERTY_REPOSITORIES = "repositories";

    /**
     * Specify whether to use Maven Central as fallback repository (in addition to other configured repositories).
     */
    String PROPERTY_USE_FALLBACK_REPOSITORIES = "useFallbackRepositories";

    /**
     * <p>Option to configure the default timeout; use a default timeout of 5 secs by default.</p>
     *
     * <p>This option allows to specify connection and request timeout default value which is used when no
     * timeout is specified at repository level in {@code settings.xml} file.</p>
     */
    String PROPERTY_TIMEOUT = "timeout";

    /**
     * Configure connection timeout value for <code>java.net.Socket#connect(SocketAddress, int)</code> operation.
     * If not specified, generic {@link #PROPERTY_TIMEOUT} is used.
     */
    String PROPERTY_SOCKET_CONNECTION_TIMEOUT = "socket.connectionTimeout";

    /**
     * Configure {@link java.net.SocketOptions#SO_TIMEOUT}.
     * If not specified, generic {@link #PROPERTY_TIMEOUT} is used.
     */
    String PROPERTY_SOCKET_SO_TIMEOUT = "socket.readTimeout";

//    /**
//     * Configure {@link java.net.SocketOptions#SO_KEEPALIVE}. Defaults to <code>false</code>
//     */
//    String PROPERTY_SOCKET_SO_KEEPALIVE = "socket.keepAlive";
//
//    /**
//     * Configure {@link java.net.SocketOptions#SO_LINGER}. Defaults to <code>-1</code>.
//     */
//    String PROPERTY_SOCKET_SO_LINGER = "socket.linger";
//
//    /**
//     * Configure {@link java.net.SocketOptions#SO_REUSEADDR}. Defaults to <code>false</code>
//     */
//    String PROPERTY_SOCKET_SO_REUSEADDRESS = "socket.reuseAddress";
//
//    /**
//     * Configure {@link java.net.SocketOptions#TCP_NODELAY}. Defaults to <code>true</code>
//     */
//    String PROPERTY_SOCKET_TCP_NODELAY = "socket.tcpNoDelay";
//
//    /**
//     * Configure buffer size for HTTP connections in:<ul>
//     *     <li>org.apache.http.impl.io.SessionInputBufferImpl#buffer</li>
//     *     <li>org.apache.http.impl.io.SessionOutputBufferImpl#buffer</li>
//     * </ul>
//     * Defaults to <code>8192</code>.
//     */
//    String PROPERTY_CONNECTION_BUFFER_SIZE = "connection.bufferSize";

    /**
     * Configure httpclient's <code>org.apache.http.impl.client.DefaultHttpRequestRetryHandler</code>.
     * Default value is <code>3</code>
     */
    String PROPERTY_CONNECTION_RETRY_COUNT = "connection.retryCount";

    /**
     * Certificate check configuration property name. Defaults to {@code true}, which means
     * "verify server certificate".
     */
    String PROPERTY_CERTIFICATE_CHECK = "certificateCheck";

    /**
     * If set to {@code true}, system properties like {@code http.proxyHost}, {@code http.proxyPort} (and related)
     * will be used to create Maven proxies and to configure Maven Resolver's {@code aether.connector.http.useSystemProperties}
     * property for HttpClient 4 (maven-resolver-transport-http).
     */
    String PROPERTY_USE_SYSTEM_PROPERTIES = "useSystemProperties";

    // ---- Options that can be specified and used for Maven Repository URLs

    /**
     * Options separator in repository url. Options can be added after a URL like this:
     * <pre>
     *     file://${user.home}/.m2/repository@id=local-repository@snapshots
     * </pre>
     */
    String SEPARATOR_OPTIONS = "@";

    /**
     * segment in repository spec that gives the name of the repo. Crucial for Aether handler.
     */
    String OPTION_ID = "id";

    /**
     * Option to mark repository as not allowing releases.
     */
    String OPTION_DISALLOW_RELEASES = "noreleases";

    /**
     * Option to mark repository as allowing snapshots.
     */
    String OPTION_ALLOW_SNAPSHOTS = "snapshots";

    /**
     * Option to mark local directory as a parent directory containing individual file-based repositories.
     * So at runtime the parent directory is scanned for subdirectories
     * and each subdirectory is used as a remote repository.
     */
    String OPTION_MULTI = "multi";

    /**
     * Option to specify release/snapshot update policy for the repository. Used to override global update policy.
     */
    String OPTION_UPDATE = "update";

    /**
     * Option to specify release update policy for the repository. Used to override global update policy.
     */
    String OPTION_RELEASES_UPDATE = "releasesUpdate";

    /**
     * Option to specify snapshot update policy for the repository. Used to override global update policy.
     */
    String OPTION_SNAPSHOTS_UPDATE = "snapshotsUpdate";

    /**
     * Option to specify release/snapshot checksum policy for the repository. Used to override global checksum policy.
     */
    String OPTION_CHECKSUM = "checksum";

    /**
     * Option to specify release checksum policy for the repository. Used to override global checksum policy.
     */
    String OPTION_RELEASES_CHECKSUM = "releasesChecksum";

    /**
     * Option to specify snapshot checksum policy for the repository. Used to override global checksum policy.
     */
    String OPTION_SNAPSHOTS_CHECKSUM = "snapshotsChecksum";

    // ---- Split repository options
    //      See: https://maven.apache.org/resolver/local-repository.html#split-local-repository
    //      See: https://github.com/ops4j/org.ops4j.pax.url/issues/417#issuecomment-1814443168
    //      These options can only be specified for file-based repositories (local repository and default repositories)

    /**
     * Option to enable split local repository for cached (remote) and local (installed) artifacts.
     */
    String OPTION_SPLIT = "split";

    /**
     * Subdirectory for split installed (local) artifacts in local repository. Defaults to "installed".
     */
    String OPTION_SPLIT_LOCAL_PREFIX = "splitLocalPrefix";

    /**
     * Subdirectory for split cached (remote) artifacts in local repository. Defaults to "cached".
     */
    String OPTION_SPLIT_REMOTE_PREFIX = "splitRemotePrefix";

    /**
     * Option to enable further splitting of locally installed subdirectory within a local repository to
     * release and snapshot versions.
     */
    String OPTION_SPLIT_LOCAL = "splitLocal";

    /**
     * Option to enable further splitting of remote cached subdirectory within a local repository to
     * release and snapshot versions.
     */
    String OPTION_SPLIT_REMOTE = "splitRemote";

    /**
     * Subdirectory for split installed and cached non-snapshot artifacts in local repository. Defaults to "releases".
     */
    String OPTION_SPLIT_RELEASES_PREFIX = "splitReleasesPrefix";

    /**
     * Subdirectory for split installed and cached snapshot artifacts in local repository. Defaults to "snapshots".
     */
    String OPTION_SPLIT_SNAPSHOTS_PREFIX = "splitSnapshotsPrefix";

    /**
     * Option to enable further splitting of remote cached subdirectory of local repository by origin remote
     * repository ID.
     */
    String OPTION_SPLIT_REMOTE_REPOSITORY = "splitRemoteRepository";

    /**
     * If split by remote repository id is effective, whether to use remote repository ID after (when {@code true})
     * of before (when {@code false}) the split segment for releases/snapshots.
     */
    String OPTION_SPLIT_REMOTE_REPOSITORY_LAST = "splitRemoteRepositoryLast";

}
