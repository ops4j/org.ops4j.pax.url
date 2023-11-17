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
 * An enumeration of constants related to maven handler.
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
     * If this environmental variable is specified, the value will be used as global Maven mirror (for all repositories)
     */
    String PROPERTY_MAVEN_MIRROR_URL_ENV = "MAVEN_MIRROR_URL";

    /**
     * If this system property is specified, the value will be used as global Maven mirror (for all repositories)
     */
    String PROPERTY_MAVEN_MIRROR_URL_SYS = "mavenMirrorUrl";

    /**
     * Warning: use only in framework properties.  If present, do not accept configuration, wait for one without this flag.
     */
    String REQUIRE_CONFIG_ADMIN_CONFIG = "requireConfigAdminConfig";

    /**
     * Add the default local repo (e.g. ~/.m2/repository) as a remote repo.  Useful when setting the local repo to be e.g. karaf system repo.
     * BE CAREFUL! if you are using mirrors, do not mirror local repos!  e.g. use <mirrorOf>external:*</mirrorOf>
     */
    String PROPERTY_LOCAL_REPO_AS_REMOTE = "defaultLocalRepoAsRemote";

    /**
     * Global update policy property name.
     * <p>
     * Provides <a href="http://maven.apache.org/settings.html">repository update policy</a> which
     * will be applied to all configured repositories.
     * <p>
     * See <a href=
     * "http://sonatype.github.com/sonatype-aether/apidocs/org/sonatype/aether/util/DefaultRepositorySystemSession.html#setUpdatePolicy%28java.lang.String%29"
     * >DefaultRepositorySystemSession</a>
     */
    String PROPERTY_GLOBAL_UPDATE_POLICY = "globalUpdatePolicy";

    /**
     * Global checksum policy property name.
     * <p>
     * Provides <a href="http://maven.apache.org/settings.html">repository checksum policy</a> which
     * will be applied to all configured repositories.
     * <p>
     * See <a href=
     * "http://sonatype.github.com/sonatype-aether/apidocs/org/sonatype/aether/util/DefaultRepositorySystemSession.html#setChecksumPolicy%28java.lang.String%29"
     * >DefaultRepositorySystemSession</a>
     */
    String PROPERTY_GLOBAL_CHECKSUM_POLICY = "globalChecksumPolicy";

    /**
     * Certificate check configuration property name.
     */
    String PROPERTY_CERTIFICATE_CHECK = "certificateCheck";
    
    /**
     * Maven settings file configuration property name.
     */
    String PROPERTY_SETTINGS_FILE = "settings";
    
    /**
     * LocalRepository configuration property name.
     */
    String PROPERTY_LOCAL_REPOSITORY = "localRepository";
    
    /**
     * DefaultRepositories configuration property name
     */
    String PROPERTY_DEFAULT_REPOSITORIES = "defaultRepositories";
    
    /**
     * Repositories configuration property name.
     */
    String PROPERTY_REPOSITORIES = "repositories";
    
    /**
     * Use fallback repositories switch configuration property name.
     */
    String PROPERTY_USE_FALLBACK_REPOSITORIES = "useFallbackRepositories";
    
    /**
     * Proxy support configuration property name.
     */
    String PROPERTY_PROXY_SUPPORT = "proxySupport";
    
    String PROPERTY_SECURITY = "security";
    
    /**
     * Option to mark repository as allowing snapshots.
     */
    String OPTION_ALLOW_SNAPSHOTS = "snapshots";

    /**
     * Option to configure the default timeout; use a default timeout of
     * 5 secs by default.
     */
    String PROPERTY_TIMEOUT = "timeout";

    /**
     * Option to set maven offline.
     */
    String PROPERTY_OFFLINE = "offline";

    /**
     * Option to mark repository as not allowing releases.
     */
    String OPTION_DISALLOW_RELEASES = "noreleases";
    
    /**
     * Option to mark path as a parent directory of repo directories. 
     * So at runtime the parent directory is scanned for subdirectories
     * and each subdirectory is used as a remote repo
     */
    String OPTION_MULTI = "multi";
    
    /**
     * Options separator in repository url.
     */
    String SEPARATOR_OPTIONS = "@";
    
    /**
     * Proxies given via property.
     * Expected layout: http:host=foo,port=8080;https:host=bar,port=9090
     */
    String PROPERTY_PROXIES = "proxies";
    
    /**
     * Default enable proxies or not if property PROPERTY_PROXY_SUPPORT is not set at all.
     */
    boolean PROPERTY_PROXY_SUPPORT_DEFAULT = true;
    
    /**
     * segment in repository spec that gives the name of the repo. Crucial for Aether handler.
     */
    String OPTION_ID = "id";

    String OPTION_UPDATE = "update";

    String OPTION_RELEASES_UPDATE = "releasesUpdate";

    String OPTION_SNAPSHOTS_UPDATE = "snapshotsUpdate";

    String OPTION_CHECKSUM = "checksum";

    String OPTION_RELEASES_CHECKSUM = "releasesChecksum";

    String OPTION_SNAPSHOTS_CHECKSUM = "snapshotsChecksum";

    /**
     * Configure {@link java.net.SocketOptions#SO_TIMEOUT}.
     * If not specified, generic {@link #PROPERTY_TIMEOUT} is used.
     */
    String PROPERTY_SOCKET_SO_TIMEOUT = "socket.readTimeout";

    /**
     * Configure {@link java.net.SocketOptions#SO_KEEPALIVE}. Defaults to <code>false</code>
     */
    String PROPERTY_SOCKET_SO_KEEPALIVE = "socket.keepAlive";

    /**
     * Configure {@link java.net.SocketOptions#SO_LINGER}. Defaults to <code>-1</code>.
     */
    String PROPERTY_SOCKET_SO_LINGER = "socket.linger";

    /**
     * Configure {@link java.net.SocketOptions#SO_REUSEADDR}. Defaults to <code>false</code>
     */
    String PROPERTY_SOCKET_SO_REUSEADDRESS = "socket.reuseAddress";

    /**
     * Configure {@link java.net.SocketOptions#TCP_NODELAY}. Defaults to <code>true</code>
     */
    String PROPERTY_SOCKET_TCP_NODELAY = "socket.tcpNoDelay";

    /**
     * Configure connection timeout value for <code>java.net.Socket#connect(SocketAddress, int)</code> operation.
     * If not specified, generic {@link #PROPERTY_TIMEOUT} is used.
     */
    String PROPERTY_SOCKET_CONNECTION_TIMEOUT = "socket.connectionTimeout";

    /**
     * Configure buffer size for HTTP connections in:<ul>
     *     <li>org.apache.http.impl.io.SessionInputBufferImpl#buffer</li>
     *     <li>org.apache.http.impl.io.SessionOutputBufferImpl#buffer</li>
     * </ul>
     * Defaults to <code>8192</code>.
     */
    String PROPERTY_CONNECTION_BUFFER_SIZE = "connection.bufferSize";

    /**
     * Configure httpclient's <code>org.apache.http.impl.client.DefaultHttpRequestRetryHandler</code>.
     * Default value is <code>3</code>
     */
    String PROPERTY_CONNECTION_RETRY_COUNT = "connection.retryCount";

    /**
     * <p>Property for configuration of non-canonical Maven behavior. If set to <code>true</code>,
     * {@link MavenResolver} will use Aether policies to determine whether already available non-SNAPSHOT
     * artifact should be redownloaded.</p>
     * <p>Default value is <code>false</code>.</p>
     */
    String PROPERTY_UPDATE_RELEASES = "updateReleases";

}
