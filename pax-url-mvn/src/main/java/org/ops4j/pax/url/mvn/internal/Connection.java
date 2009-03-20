/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.url.mvn.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.util.xml.XmlUtils;

/**
 * An URLConnextion that supports mvn: protocol.<br/>
 * Syntax:<br>
 * mvn:[repository_url!]groupId/artifactId[/version[/type]]<br/>
 * where:<br/>
 * - repository_url = an url that points to a maven 2 repository; optional, if not sepecified the repositories are
 * resolved based on the repository/localRepository.<br/>
 * - groupId = group id of maven artifact; mandatory<br/>
 * - artifactId = artifact id of maven artifact; mandatory<br/>
 * - version = version of maven artifact; optional, if not specified uses LATEST and will try to resolve the version
 * from available maven metadata. If version is a SNAPSHOT version, SNAPSHOT will be resolved from available maven
 * metadata<br/>
 * - type = type of maven artifact; optional, if not specified uses JAR<br/>
 * Examples:<br>
 * mvn:http://repository.ops4j.org/maven2!org.ops4j.pax.runner/runner/0.4.0 - an artifact from an http repository<br/>
 * mvn:http://user:password@repository.ops4j.org/maven2!org.ops4j.pax.runner/runner/0.4.0 - an artifact from an http
 * repository with authentication<br/>
 * mvn:file://c:/localRepo!org.ops4j.pax.runner/runner/0.4.0 - an artifact from a directory<br/>
 * mvn:jar:file://c:/repo.zip!/repository!org.ops4j.pax.runner/runner/0.4.0 - an artifact from a zip file<br/>
 * mvn:org.ops4j.pax.runner/runner/0.4.0 - an artifact that will be resolved based on the configured repositories<br/>
 * <br/>
 * The service can be configured in two ways: via configuration admin if available and via framework/system properties
 * where the configuration via config admin has priority.<br/>
 * Service configuration:<br/>
 * - org.ops4j.pax.url.mvn.settings = the path to settings.xml;<br/>
 * - org.ops4j.pax.url.mvn.localRepository = the path to local repository directory;<br>
 * - org.ops4j.pax.url.mvn.repository =  a comma separated list for repositories urls;<br/>
 * - org.ops4j.pax.url.mvn.certicateCheck = true/false if the SSL certificate check should be done.
 * Default false.
 *
 * @author Alin Dreghiciu
 * @since August 10, 2007
 */
public class Connection
    extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Connection.class );
    /**
     * 2 spacess indent;
     */
    private static final String Ix2 = "  ";
    /**
     * 4 spacess indent;
     */
    private static final String Ix4 = "    ";

    /**
     * Parsed url.
     */
    private Parser m_parser;
    /**
     * Service configuration.
     */
    private final MavenConfiguration m_configuration;

    /**
     * Creates a new connection.
     *
     * @param url           the url; cannot be null.
     * @param configuration service configuration; cannot be null
     *
     * @throws MalformedURLException in case of a malformed url
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
     * Does nothing.
     *
     * @see java.net.URLConnection#connect()
     */
    @Override
    public void connect()
    {
        // do nothing
    }

    /**
     * Returns the input stream denoted by the url.<br/>
     * If the url does not contain a repository the resource is searched in every repository if available, in the order
     * provided by the repository setting.
     *
     * @return the input stream for the resource denoted by url
     *
     * @throws IOException in case of an exception during accessing the resource
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        LOG.debug( "Resolving [" + url.toExternalForm() + "]" );
        final Set<DownloadableArtifact> defaultDownloadables = collectDefaultPossibleDownloads();
        if( LOG.isTraceEnabled() )
        {
            LOG.trace( "Possible default download locations for [" + url.toExternalForm() + "]" );
            for( DownloadableArtifact artifact : defaultDownloadables )
            {
                LOG.trace( "  " + artifact );
            }
        }
        for( DownloadableArtifact artifact : defaultDownloadables )
        {
            LOG.trace( "Downloading [" + artifact + "]" );
            try
            {
                m_configuration.enableProxy( artifact.getArtifactURL() );
                return artifact.getInputStream();
            }
            catch( IOException ignore )
            {
                // go on with next repository
                LOG.debug( Ix2 + "Could not download [" + artifact + "]" );
                LOG.trace( Ix2 + "Reason [" + ignore.getClass().getName() + ": " + ignore.getMessage() + "]" );
            }
        }
        final Set<DownloadableArtifact> downloadables = collectPossibleDownloads();
        if( LOG.isTraceEnabled() )
        {
            LOG.trace( "Possible download locations for [" + url.toExternalForm() + "]" );
            for( DownloadableArtifact artifact : downloadables )
            {
                LOG.trace( "  " + artifact );
            }
        }
        for( DownloadableArtifact artifact : downloadables )
        {
            LOG.trace( "Downloading [" + artifact + "]" );
            try
            {
                m_configuration.enableProxy( artifact.getArtifactURL() );
                return artifact.getInputStream();
            }
            catch( IOException ignore )
            {
                // go on with next repository
                LOG.debug( Ix2 + "Could not download [" + artifact + "]" );
                LOG.trace( Ix2 + "Reason [" + ignore.getClass().getName() + ": " + ignore.getMessage() + "]" );
            }
        }
        // no artifact found
        throw new RuntimeException(
            "URL [" + url.toExternalForm() + "] could not be resolved. (enable TRACE logging for details)"
        );
    }

    /**
     * Searches all available repositories for possible artifacts to download. The returned set of downloadable
     * artifacts (never null, but maybe empty) will be sorted descending by version of the artifact and by positon of
     * repository in the list of repositories to be searched.
     *
     * @return a non null sorted set of artifacts
     *
     * @throws java.net.MalformedURLException re-thrown
     */
    private Set<DownloadableArtifact> collectPossibleDownloads()
        throws MalformedURLException
    {
        final List<MavenRepositoryURL> repositories = new ArrayList<MavenRepositoryURL>();
        repositories.addAll( m_configuration.getRepositories() );
        // if the url contains a prefered repository add that repository as the first repository to be searched
        if( m_parser.getRepositoryURL() != null )
        {
            repositories.add( 0, m_parser.getRepositoryURL() );
        }
        return doCollectPossibleDownloads( repositories );
    }

    /**
     * Search the default repositories for possible artifacts to download.
     */
    private Set<DownloadableArtifact> collectDefaultPossibleDownloads()
        throws MalformedURLException
    {
        return doCollectPossibleDownloads( m_configuration.getDefaultRepositories() );
    }

    private Set<DownloadableArtifact> doCollectPossibleDownloads( final List<MavenRepositoryURL> repositories )
        throws MalformedURLException
    {
        final Set<DownloadableArtifact> downloadables = new TreeSet<DownloadableArtifact>( new DownloadComparator() );

        // find artifact type
        final boolean isLatest = m_parser.getVersion().contains( "LATEST" );
        final boolean isSnapshot = m_parser.getVersion().endsWith( "SNAPSHOT" );
        VersionRange versionRange = null;
        if( !isLatest && !isSnapshot )
        {
            try
            {
                versionRange = new VersionRange( m_parser.getVersion() );
            }
            catch( Exception ignore )
            {
                // well, we do not have a range of versions
            }
        }
        final boolean isVersionRange = versionRange != null;
        final boolean isExactVersion = !( isLatest || isSnapshot || isVersionRange );

        int priority = 0;
        for( MavenRepositoryURL repositoryURL : repositories )
        {
            LOG.debug( "Collecting versions from repository [" + repositoryURL + "]" );
            priority++;
            try
            {
                if( isExactVersion )
                {
                    downloadables.add( resolveExactVersion( repositoryURL, priority ) );
                }
                else if( isSnapshot )
                {
                    final DownloadableArtifact snapshot =
                        resolveSnapshotVersion( repositoryURL, priority, m_parser.getVersion() );
                    downloadables.add( snapshot );
                    // if we have a local built snapshot we skip the rest of repositories
                    if( snapshot.isLocalSnapshotBuild() )
                    {
                        break;
                    }
                }
                else
                {
                    final Document metadata = getMetadata( repositoryURL.toURL(),
                                                           new String[]
                                                               {
                                                                   m_parser.getArtifactLocalMetdataPath(),
                                                                   m_parser.getArtifactMetdataPath()
                                                               }
                    );
                    if( isLatest )
                    {
                        downloadables.add( resolveLatestVersion( metadata, repositoryURL, priority ) );
                    }
                    else
                    {
                        downloadables.addAll( resolveRangeVersions( metadata, repositoryURL, priority, versionRange ) );
                    }
                }
            }
            catch( IOException ignore )
            {
                // if metadata cannot be found we go on with the next repository. Maybe we have better luck.
                LOG.debug( Ix2 + "Skipping repository [" + repositoryURL + "], reason: " + ignore.getMessage() );
            }
        }
        return downloadables;
    }

    /**
     * Returns maven metadata by looking first for a local metatdata xml file and then for a remote one.
     * If no metadata file is found or cannot be used an IOException is thrown.
     *
     * @param repositoryURL     url of the repository from where the metadata should be parsed
     * @param metadataLocations array of location paths to try as metadata
     *
     * @return parsed xml document for the metadata file
     *
     * @throws java.io.IOException if:
     *                             metadata file cannot be located
     */
    private Document getMetadata( final URL repositoryURL,
                                  final String[] metadataLocations )
        throws IOException
    {
        LOG.debug( Ix2 + "Resolving metadata" );
        InputStream inputStream = null;
        String foundLocation = null;
        for( String location : metadataLocations )
        {
            try
            {
                // first try to get the artifact local metadata
                inputStream = prepareInputStream( repositoryURL, location );
                // get out at first found location
                foundLocation = location;
                LOG.trace( Ix4 + "Metadata found: [" + location + "]" );
                break;
            }
            catch( IOException ignore )
            {
                LOG.trace( Ix4 + "Metadata not found: [" + location + "]" );
            }
        }
        if( inputStream == null )
        {
            throw new IOException( "Metadata not found in repository [" + repositoryURL + "]" );
        }
        try
        {
            return XmlUtils.parseDoc( inputStream );
        }
        catch( ParserConfigurationException e )
        {
            throw initIOException( "Metadata [" + foundLocation + "] could not be parsed.", e );
        }
        catch( SAXException e )
        {
            throw initIOException( "Metadata [" + foundLocation + "] could not be parsed.", e );
        }
    }

    /**
     * Returns a downloadable artifact where the version is fully specified.
     *
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     *
     * @return a downloadable artifact
     *
     * @throws IOException re-thrown
     */
    private DownloadableArtifact resolveExactVersion( final MavenRepositoryURL repositoryURL,
                                                      final int priority )
        throws IOException
    {
        if( !repositoryURL.isReleasesEnabled() )
        {
            throw new IOException( "Releases not enabled" );
        }
        LOG.debug( Ix2 + "Resolving exact version" );
        return new DownloadableArtifact(
            m_parser.getVersion(),
            priority,
            repositoryURL.toURL(),
            m_parser.getArtifactPath(),
            false, // no local built snapshot
            m_configuration.getCertificateCheck()
        );
    }

    /**
     * Resolves the latest version of the artifact.
     *
     * @param metadata      parsed metadata xml
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     *
     * @return a downloadable artifact or throw an IOException if latest version cannot be determined.
     *
     * @throws IOException if the artifact could not be resolved
     */
    private DownloadableArtifact resolveLatestVersion( final Document metadata,
                                                       final MavenRepositoryURL repositoryURL,
                                                       final int priority )
        throws IOException
    {
        LOG.debug( Ix2 + "Resolving latest version" );
        final String version = XmlUtils.getTextContentOfElement( metadata, "versioning/versions/version[last]" );
        if( version != null )
        {
            if( version.endsWith( "SNAPSHOT" ) )
            {
                return resolveSnapshotVersion( repositoryURL, priority, version );
            }
            else
            {
                return new DownloadableArtifact(
                    version,
                    priority,
                    repositoryURL.toURL(),
                    m_parser.getArtifactPath( version ),
                    false, // no local built snapshot
                    m_configuration.getCertificateCheck()
                );
            }
        }
        throw new IOException( "LATEST version could not be resolved." );
    }

    /**
     * Resolves snapshot version of the artifact.
     * Snapshot versions are resolved by parsing the metadata within the directory that contains the version as:
     * 1. if the metadata containes entries like "versioning/snapshot/timestamp (most likely on remote repos) it will
     * use the timestamp and buildnumber to point the real version
     * 2. if the metatdata does not contain the above (most likely a local repo) it will use as version the
     * versioning/lastUpdated
     *
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     * @param version       snapshot version to resolve
     *
     * @return an input stream to the artifact
     *
     * @throws IOException if the artifact could not be resolved
     */
    private DownloadableArtifact resolveSnapshotVersion( final MavenRepositoryURL repositoryURL,
                                                         final int priority,
                                                         final String version )
        throws IOException
    {
        if( !repositoryURL.isSnapshotsEnabled() )
        {
            throw new IOException( "Snapshots not enabled" );
        }
        LOG.debug( Ix2 + "Resolving snapshot version [" + version + "]" );
        try
        {
            final Document snapshotMetadata = getMetadata( repositoryURL.toURL(),
                                                           new String[]
                                                               {
                                                                   m_parser.getVersionLocalMetadataPath( version ),
                                                                   m_parser.getVersionMetadataPath( version )
                                                               }
            );
            final String timestamp =
                XmlUtils.getTextContentOfElement( snapshotMetadata, "versioning/snapshot/timestamp" );
            final String buildNumber =
                XmlUtils.getTextContentOfElement( snapshotMetadata, "versioning/snapshot/buildNumber" );
            final String localSnapshot =
                XmlUtils.getTextContentOfElement( snapshotMetadata, "versioning/snapshot/localCopy" );
            if( timestamp != null && buildNumber != null )
            {
                return new DownloadableArtifact(
                    m_parser.getSnapshotVersion( version, timestamp, buildNumber ),
                    priority,
                    repositoryURL.toURL(),
                    m_parser.getSnapshotPath( version, timestamp, buildNumber ),
                    localSnapshot != null,
                    m_configuration.getCertificateCheck()
                );
            }
            else
            {
                String lastUpdated = XmlUtils.getTextContentOfElement( snapshotMetadata, "versioning/lastUpdated" );
                if( lastUpdated != null )
                {
                    // last updated should contain in the first 8 chars the date and then the time,
                    // fact that is not compatible with timeStamp from remote repos which has a "." after date
                    if( lastUpdated.length() > 8 )
                    {
                        lastUpdated = lastUpdated.substring( 0, 8 ) + "." + lastUpdated.substring( 8 );
                        return new DownloadableArtifact(
                            m_parser.getSnapshotVersion( version, lastUpdated, "0" ),
                            priority,
                            repositoryURL.toURL(),
                            m_parser.getArtifactPath( version ),
                            localSnapshot != null,
                            m_configuration.getCertificateCheck()
                        );
                    }
                }
            }
        }
        catch( IOException ignore )
        {
            // in this case we could not find any metadata so try to get the *-SNAPSHOT file directly
        }
        return resolveExactVersion( repositoryURL, priority );
    }

    /**
     * Resolves all versions that fits the provided range.
     *
     * @param metadata      parsed metadata xml
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     * @param versionRange  version range to fulfill
     *
     * @return list of downloadable artifacts that match the range
     *
     * @throws IOException re-thrown
     */
    private List<DownloadableArtifact> resolveRangeVersions( final Document metadata,
                                                             final MavenRepositoryURL repositoryURL,
                                                             final int priority,
                                                             final VersionRange versionRange )
        throws IOException
    {
        LOG.debug( Ix2 + "Resolving versions in range [" + versionRange + "]" );
        final List<DownloadableArtifact> downladables = new ArrayList<DownloadableArtifact>();
        final List<Element> elements = XmlUtils.getElements( metadata, "versioning/versions/version" );
        if( elements != null && elements.size() > 0 )
        {
            for( Element element : elements )
            {
                final String versionString = XmlUtils.getTextContent( element );
                if( versionString != null )
                {
                    final Version version = new Version( versionString );
                    if( versionRange.includes( version ) )
                    {
                        if( versionString.endsWith( "SNAPSHOT" ) )
                        {
                            downladables.add(
                                resolveSnapshotVersion( repositoryURL, priority, versionString )
                            );
                        }
                        else
                        {
                            downladables.add(
                                new DownloadableArtifact(
                                    versionString,
                                    priority,
                                    repositoryURL.toURL(),
                                    m_parser.getArtifactPath( versionString ),
                                    false, // no local built snapshot
                                    m_configuration.getCertificateCheck()
                                )
                            );
                        }
                    }
                }
            }
        }
        return downladables;
    }

    /**
     * @param repositoryURL url to reporsitory
     * @param path          a path to the artifact jar file
     *
     * @return prepared input stream
     *
     * @throws IOException re-thrown
     * @see org.ops4j.net.URLUtils#prepareInputStream(java.net.URL,boolean)
     */
    private InputStream prepareInputStream( URL repositoryURL, final String path )
        throws IOException
    {
        String repository = repositoryURL.toExternalForm();
        if( !repository.endsWith( Parser.FILE_SEPARATOR ) )
        {
            repository = repository + Parser.FILE_SEPARATOR;
        }
        m_configuration.enableProxy( repositoryURL );
        return URLUtils.prepareInputStream( new URL( repository + path ), !m_configuration.getCertificateCheck() );
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
     * Sorting comparator for downladable artifacts.
     * The sorting is done by:
     * 1. descending version
     * 2. ascending priority.
     */
    private static class DownloadComparator
        implements Comparator<DownloadableArtifact>
    {

        public int compare( final DownloadableArtifact first,
                            final DownloadableArtifact second )
        {
            // first descending by version
            int result = -1 * first.getVersion().compareTo( second.getVersion() );
            if( result == 0 )
            {
                // then ascending by priority
                if( first.getPriority() < second.getPriority() )
                {
                    result = -1;
                }
                else if( first.getPriority() > second.getPriority() )
                {
                    result = 1;
                }
            }
            return result;
        }

    }
}
