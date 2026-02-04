/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.url.mvn.internal.config;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>An URL-like information about Maven repository (local, default or remote) used in Pax URL Aether. Repository
 * URL may be a part of full {@code mvn:} URL, but is also used simply as Maven repository in configuration.</p>
 *
 * <p>There are 3 kinds of repositories:<ul>
 *     <li>local repository - there should be only one, configured using
 *     {@link ServiceConstants#PROPERTY_LOCAL_REPOSITORY}, but may also use standard Maven {@code maven.repo.local}
 *     property, be read from {@code settings.xml} or fall back to {@code ~/.m2/repository} if not specified. </li>
 *     <li>default repositories - these are local, read-only locations (like {@code $KARAF_HOME/system}) which are
 *     treated like local repositories during resolution without remote repositories. In short words - if something is
 *     available there, it is treated as resolved. If not - skip to next default repository or end the process
 *     with normal resolution using remote repositories and local one (for caching of the downloads)</li>
 *     <li>remote repositories - these are usually remote, HTTP repositories, where remote queries are sent to check
 *     if the artifact being searched for is available. If so, the artifact is downloaded and cached in local
 *     repository.</li>
 * </ul></p>
 *
 * <p>Local repository and default repositories may be configured as split repositories (since Pax URL 3 and Maven
 * Resolver 1.9).</p>
 *
 * <p>The repository URL format looks like this:<pre>
 *     url := scheme:[//authority]/path[options]
 *     authority := [user:password@]host[:port]
 *     path := '/' - separated path
 *     options := '@'option
 *     option := key=value | key
 * </pre>
 * For example: {@code https://user:password@remote-repository.example.io/maven@id=remote1@snapshots@split=true}.
 * For {@code file:} URLs, there are two confusing variants (let's not talk about Windows-specific ones):<ul>
 *     <li>{@code file:///path/to/location}</li>
 *     <li>{@code file:/path/to/location}</li>
 * </ul>
 * Three-slashes version means "empty authority part", while single slash means "just path without authority URL part".
 * </p>
 *
 * <p>The set of options used depends on whether we use local, default or remote repository. For local/default ones
 * we may use:<ul>
 *     <li>{@link ServiceConstants#OPTION_ID}</li>
 *     <li>{@link ServiceConstants#OPTION_MULTI}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_LOCAL}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_REMOTE}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_LOCAL_PREFIX}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_REMOTE_PREFIX}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_RELEASES_PREFIX}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_SNAPSHOTS_PREFIX}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_REMOTE_REPOSITORY}</li>
 *     <li>{@link ServiceConstants#OPTION_SPLIT_REMOTE_REPOSITORY_LAST}</li>
 * </ul>
 * For remote ones:<ul>
 *     <li>{@link ServiceConstants#OPTION_ID}</li>
 *     <li>{@link ServiceConstants#OPTION_DISALLOW_RELEASES}</li>
 *     <li>{@link ServiceConstants#OPTION_ALLOW_SNAPSHOTS}</li>
 *     <li>{@link ServiceConstants#OPTION_UPDATE}</li>
 *     <li>{@link ServiceConstants#OPTION_CHECKSUM}</li>
 *     <li>{@link ServiceConstants#OPTION_RELEASES_UPDATE}</li>
 *     <li>{@link ServiceConstants#OPTION_SNAPSHOTS_UPDATE}</li>
 *     <li>{@link ServiceConstants#OPTION_RELEASES_CHECKSUM}</li>
 *     <li>{@link ServiceConstants#OPTION_SNAPSHOTS_CHECKSUM}</li>
 * </ul></p>
 *
 * @author Alin Dreghiciu
 * @author Guillaume Nodet
 * @since 0.2.1, February 07, 2008
 */
public class MavenRepositoryURL
{
    private static final Logger LOG = LoggerFactory.getLogger( MavenRepositoryURL.class );

    /**
     * Repository Id. Used to identify a repository and reference it from {@code setting.xml} (mirror id, repository
     * id, server id).
     */
    private final String m_id;

    /**
     * Repository URI.
     */
    private final URI m_repositoryURI;

    /**
     * Decoded username from {@link URI#getUserInfo()}
     */
    private final String m_username;

    /**
     * Decoded password from {@link URI#getUserInfo()}
     */
    private final char[] m_password;

    /**
     * Repository file (only if URL is a file URL). Used for local and default repositories.
     */
    private final File m_file;

    /**
     * True if the repository contains releases. Maps to {@code <repository>/<releases>/<enabled>}.
     */
    private final boolean m_releasesEnabled;

    /**
     * Repository update policy. Maps to {@code <repository>/<releases>/<updatePolicy>}.
     */
    private final String m_releasesUpdatePolicy;

    /**
     * Repository checksum policy. Maps to {@code <repository>/<releases>/<checksumPolicy>}.
     */
    private final String m_releasesChecksumPolicy;

    /**
     * True if the repository contains snapshots. Maps to {@code <repository>/<snapshots>/<enabled>}.
     */
    private final boolean m_snapshotsEnabled;

    /**
     * Repository update policy. Maps to {@code <repository>/<releases>/<updatePolicy>}.
     */
    private final String m_snapshotsUpdatePolicy;

    /**
     * Repository checksum policy. Maps to {@code <repository>/<snapshots>/<checksumPolicy>}.
     */
    private final String m_snapshotsChecksumPolicy;

    /**
     * Whether the repository is actually a parent directory of multiple individual repositories.
     */
    private final boolean m_multi;

    /**
     * Whether the repository is split into locally installed and cached remote artifacts.
     */
    private final boolean m_split;

    /**
     * The subdirectory name for locally installed artifacts. Defaults to "installed".
     */
    private final String m_splitLocalPrefix;

    /**
     * The subdirectory name for cached remote artifacts. Defaults to "cached".
     */
    private final String m_splitRemotePrefix;

    /**
     * Whether the locally installed artifacts are split into snapshot and released versions.
     */
    private final boolean m_splitLocal;

    /**
     * Whether the cached remote artifacts are split into snapshot and released versions.
     */
    private final boolean m_splitRemote;

    /**
     * The subdirectory name for release artifacts (non-SNAPSHOTs) - both installed and cached.
     */
    private final String m_splitReleasesPrefix;

    /**
     * The subdirectory name for snapshot artifacts - both installed and cached.
     */
    private final String m_splitSnapshotsPrefix;

    /**
     * Whether cached remote artifacts subdirectory should be further split by origin repository ID.
     */
    private final boolean m_splitRemoteRepository;

    /**
     * Whether the origin remote repository ID should be used after (true) or before (false) the subdirectory for
     * releases/snapshots of the remote cache.
     */
    private final boolean m_splitRemoteRepositoryLast;

    /**
     * Creates a maven repository URL bases on a string spec. The path can be marked with @snapshots and/or @noreleases
     * (not case sensitive).
     *
     * @param repositorySpec url spec of repository
     *
     * @throws MalformedURLException if spec contains a malformed maven repository url
     * @throws NullArgumentException if repository spec is null or empty
     */
    public MavenRepositoryURL( final String repositorySpec )
        throws MalformedURLException
    {
        NullArgumentException.validateNotEmpty( repositorySpec, true, "Repository spec" );

        final String[] segments = repositorySpec.split( ServiceConstants.SEPARATOR_OPTIONS );
        final StringBuilder urlBuilder = new StringBuilder();
        boolean snapshotEnabled = false;
        boolean releasesEnabled = true;
        boolean multi = false;

        String name = null;

        // no defaults for any of these. Will be set from global configuration when needed
        String update = null;
        String updateReleases = null;
        String updateSnapshots = null;
        String checksum = null;
        String checksumReleases = null;
        String checksumSnapshots = null;

        boolean split = false;
        boolean splitLocal = false;
        boolean splitRemote = false;
        boolean splitRemoteRepository = false;
        boolean splitRemoteRepositoryLast = false;
        String splitLocalPrefix = "installed";
        String splitRemotePrefix = "cached";
        String splitReleasesPrefix = "releases";
        String splitSnapshotsPrefix = "snapshots";

        for( int i = 0; i < segments.length; i++ )
        {
            String segment = segments[i].trim(); 
            if( segment.equalsIgnoreCase( ServiceConstants.OPTION_ALLOW_SNAPSHOTS ) )
            {
                snapshotEnabled = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_DISALLOW_RELEASES ) )
            {
                releasesEnabled = false;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_MULTI ) )
            {
                multi = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_SPLIT ) )
            {
                split = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_SPLIT_LOCAL ) )
            {
                splitLocal = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_SPLIT_REMOTE ) )
            {
                splitRemote = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_SPLIT_REMOTE_REPOSITORY ) )
            {
                splitRemoteRepository = true;
            }
            else if( segment.equalsIgnoreCase( ServiceConstants.OPTION_SPLIT_REMOTE_REPOSITORY_LAST ) )
            {
                splitRemoteRepositoryLast = true;
            }
            else if( segment.startsWith( ServiceConstants.OPTION_ID + "=" ) )
            {
                try {
                    name = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_RELEASES_UPDATE + "=" ) )
            {
                try {
                    updateReleases = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SNAPSHOTS_UPDATE + "=" ) )
            {
                try {
                    updateSnapshots = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_UPDATE + "=" ) )
            {
                try {
                    update = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_RELEASES_CHECKSUM + "=" ) )
            {
                try {
                    checksumReleases = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SNAPSHOTS_CHECKSUM + "=" ) )
            {
                try {
                    checksumSnapshots = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_CHECKSUM + "=" ) )
            {
                try {
                    checksum = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SPLIT_LOCAL_PREFIX + "=" ) )
            {
                try {
                    splitLocalPrefix = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SPLIT_REMOTE_PREFIX + "=" ) )
            {
                try {
                    splitRemotePrefix = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SPLIT_RELEASES_PREFIX + "=" ) )
            {
                try {
                    splitReleasesPrefix = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else if( segment.startsWith( ServiceConstants.OPTION_SPLIT_SNAPSHOTS_PREFIX + "=" ) )
            {
                try {
                    splitSnapshotsPrefix = segments[ i ].split( "=" )[1].trim();
                } catch (Exception e) {
                    LOG.warn( "Problem with segment " + segments[i] + " in " + repositorySpec );
                }
            }
            else
            {
                if( i > 0 )
                {
                    urlBuilder.append( ServiceConstants.SEPARATOR_OPTIONS );
                }
                urlBuilder.append( segments[ i ] );
            }
        }
        String spec = urlBuilder.toString().trim();
        spec = spec.replaceAll("\\\\", "/");
        spec = spec.replaceAll("%5C", "/");
        if (!spec.endsWith("/")) {
            spec += "/";
        }
        m_repositoryURI = URI.create(spec);

        String credentials = m_repositoryURI.getRawUserInfo();
        if (credentials != null) {
            LOG.warn("Repository spec \"{}\" contains user information." +
                    " It is recommended to specify server credentials in external settings.xml file.", spec);

            // ":" is not a specification defined user:password separator, but we will use it. If there are more
            // ":" characters, we can't effectively determine what is the user name and what is the password
            int colon = credentials.indexOf(':');
            if (colon != -1 && credentials.indexOf(':', colon + 1) != -1) {
                LOG.warn("Multiple ':' separators found in user info part of {}." +
                        " Can't determine user credential. Please encode non-separator ':' using '%3A'.", spec);
                m_username = null;
                m_password = null;
            } else {
                String username;
                String password;
                if (colon != -1) {
                    // user and password
                    username = credentials.substring(0, colon);
                    password = credentials.substring(colon + 1);
                } else {
                    username = credentials;
                    password = null;
                }
                // because user may have encoded ':' as '%3A', we'll always decode it ONCE
                try {
                    m_username = URLDecoder.decode(username, StandardCharsets.UTF_8.name());
                    if (password != null) {
                        m_password = URLDecoder.decode(password, StandardCharsets.UTF_8.name()).toCharArray();
                    } else {
                        m_password = null;
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        } else {
            m_username = null;
            m_password = null;
        }

        m_snapshotsEnabled = snapshotEnabled;
        m_releasesEnabled = releasesEnabled;
        m_multi = multi;
        if (name == null) {
            String warn = "Repository spec " + spec + " does not contain an identifier. Give your repository a name, for example: " + repositorySpec + "@id=MyName";
            LOG.warn( warn );
            name = "repo_" + spec.hashCode();
        }
        m_id = name;
        m_releasesUpdatePolicy = updateReleases != null ? updateReleases : update;
        m_snapshotsUpdatePolicy = updateSnapshots != null ? updateSnapshots : update;
        m_releasesChecksumPolicy = checksumReleases != null ? checksumReleases : checksum;
        m_snapshotsChecksumPolicy = checksumSnapshots != null ? checksumSnapshots : checksum;

        m_split = split;
        m_splitLocal = splitLocal;
        m_splitRemote = splitRemote;
        m_splitRemoteRepository = splitRemoteRepository;
        m_splitRemoteRepositoryLast = splitRemoteRepositoryLast;
        m_splitLocalPrefix = splitLocalPrefix;
        m_splitRemotePrefix = splitRemotePrefix;
        m_splitReleasesPrefix = splitReleasesPrefix;
        m_splitSnapshotsPrefix = splitSnapshotsPrefix;

        if( m_repositoryURI.getScheme().equals( "file" ) )
        {
            try
            {
                // You must transform to URI to decode the path (manage a path with a space or non
                // us character)
                // like D:/documents%20and%20Settings/SESA170017/.m2/repository
                // the path can be store in path part or in scheme specific part (if is relatif
                // path)
                // the anti-slash character is not a valid character for uri.
                spec = spec.replaceAll( "\\\\", "/" );
                spec = spec.replaceAll( " ", "%20" );
                URI uri = new URI( spec );
                String path = uri.getPath();
                if( path == null )
                    path = uri.getSchemeSpecificPart();
                m_file = new File( path );

            }
            catch ( URISyntaxException e )
            {
                throw new MalformedURLException( e.getMessage() );
            }
        }
        else
        {
            m_file = null;
        }
    }

    /**
     * Create repository URL with other repositor URL as parent and {@code child} which should be child
     * directory of parent.
     *
     * @param parent
     * @param child
     */
    public MavenRepositoryURL(MavenRepositoryURL parent, File child) {
        if (parent.getFile() == null) {
            throw new IllegalArgumentException("Can't create child MavenRepositoryURL which is not a file (parent: " + parent + ")");
        }
        if (!parent.getFile().equals(child.getParentFile())) {
            throw new IllegalArgumentException(child + " is not a subdirectory of " + parent);
        }

        this.m_id = parent.getId() + "/" + child.getName();
        this.m_repositoryURI = child.toURI();
        this.m_username = parent.m_username;
        this.m_password = parent.m_password;
        this.m_file = child;
        this.m_releasesEnabled = parent.m_releasesEnabled;
        this.m_releasesUpdatePolicy = parent.m_releasesUpdatePolicy;
        this.m_releasesChecksumPolicy = parent.m_releasesChecksumPolicy;
        this.m_snapshotsEnabled = parent.m_snapshotsEnabled;
        this.m_snapshotsUpdatePolicy = parent.m_snapshotsUpdatePolicy;
        this.m_snapshotsChecksumPolicy = parent.m_snapshotsChecksumPolicy;
        this.m_multi = false; // obvious
        this.m_split = parent.m_split;
        this.m_splitLocalPrefix = parent.m_splitLocalPrefix;
        this.m_splitRemotePrefix = parent.m_splitRemotePrefix;
        this.m_splitLocal = parent.m_splitLocal;
        this.m_splitRemote = parent.m_splitRemote;
        this.m_splitReleasesPrefix = parent.m_splitReleasesPrefix;
        this.m_splitSnapshotsPrefix = parent.m_splitSnapshotsPrefix;
        this.m_splitRemoteRepository = parent.m_splitRemoteRepository;
        this.m_splitRemoteRepositoryLast = parent.m_splitRemoteRepositoryLast;
    }

    /**
     * Getter.
     *
     * @return repository id
     */
    public String getId()
    {
        return m_id;
    }

    /**
     * Getter.
     *
     * @return repository URI
     */
    public URI getURI()
    {
        return m_repositoryURI;
    }

    public String getUsername() {
        return m_username;
    }

    public char[] getPassword() {
        return m_password;
    }

    /**
     * Getter.
     *
     * @return repository file
     */
    public File getFile()
    {
        return m_file;
    }

    /**
     * Getter.
     *
     * @return true if the repository contains releases
     */
    public boolean isReleasesEnabled()
    {
        return m_releasesEnabled;
    }

    /**
     * Getter.
     *
     * @return true if the repository contains snapshots
     */
    public boolean isSnapshotsEnabled()
    {
        return m_snapshotsEnabled;
    }

    public String getReleasesUpdatePolicy() {
        return m_releasesUpdatePolicy;
    }

    public String getSnapshotsUpdatePolicy() {
        return m_snapshotsUpdatePolicy;
    }

    public String getReleasesChecksumPolicy() {
        return m_releasesChecksumPolicy;
    }

    public String getSnapshotsChecksumPolicy() {
        return m_snapshotsChecksumPolicy;
    }

    /**
     * Getter.
     *
     * @return true if the repository is a parent path of repos
     */
    public boolean isMulti()
    {
        return m_multi;
    }

    /**
     * Getter.
     *
     * @return if the repository is a file based repository.
     */
    public boolean isFileRepository()
    {
        return m_file != null;
    }

    public boolean isSplit() {
        return m_split;
    }

    public String getSplitLocalPrefix() {
        return m_splitLocalPrefix;
    }

    public String getSplitRemotePrefix() {
        return m_splitRemotePrefix;
    }

    public boolean isSplitLocal() {
        return m_splitLocal;
    }

    public boolean isSplitRemote() {
        return m_splitRemote;
    }

    public String getSplitReleasesPrefix() {
        return m_splitReleasesPrefix;
    }

    public String getSplitSnapshotsPrefix() {
        return m_splitSnapshotsPrefix;
    }

    public boolean isSplitRemoteRepository() {
        return m_splitRemoteRepository;
    }

    public boolean isSplitRemoteRepositoryLast() {
        return m_splitRemoteRepositoryLast;
    }

    @Override
    public String toString() {
        return m_repositoryURI.toString() + " {id=" + m_id + "}";
    }

}
