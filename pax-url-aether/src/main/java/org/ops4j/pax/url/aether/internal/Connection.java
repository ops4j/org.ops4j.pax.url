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
package org.ops4j.pax.url.aether.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * An URLConnextion that supports aether: protocol.<br/>
 * Syntax:<br>
 * aether:[repository_url!]groupId/artifactId[/version[/type]]<br/>
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
 * aether:http://repository.ops4j.org/aether-releases!org.ops4j.pax.runner/runner/0.4.0 - an artifact from an http repository<br/>
 * aether:http://user:password@repository.ops4j.org/aether-releases!org.ops4j.pax.runner/runner/0.4.0 - an artifact from an http
 * repository with authentication<br/>
 * aether:file://c:/localRepo!org.ops4j.pax.runner/runner/0.4.0 - an artifact from a directory<br/>
 * aether:jar:file://c:/repo.zip!/repository!org.ops4j.pax.runner/runner/0.4.0 - an artifact from a zip file<br/>
 * aether:org.ops4j.pax.runner/runner/0.4.0 - an artifact that will be resolved based on the configured repositories<br/>
 * <br/>
 * The service can be configured in two ways: via configuration admin if available and via framework/system properties
 * where the configuration via config admin has priority.<br/>
 * Service configuration:<br/>
 * - org.ops4j.pax.url.aether.settings = the path to settings.xml;<br/>
 * - org.ops4j.pax.url.aether.localRepository = the path to local repository directory;<br>
 * - org.ops4j.pax.url.aether.repository =  a comma separated list for repositories urls;<br/>
 * - org.ops4j.pax.url.aether.certicateCheck = true/false if the SSL certificate check should be done.
 * Default false.
 *
 * @author Toni Menzel
 * @author Alin Dreghiciu
 * @since September 10, 2010
 */
public class Connection
        extends URLConnection {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(Connection.class);
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
    private AetherBasedResolver m_aetherBasedResolver;

    /**
     * Creates a new connection.
     *
     * @param url           the url; cannot be null.
     * @param configuration service configuration; cannot be null
     * @throws MalformedURLException in case of a malformed url
     */
    public Connection(final URL url, final MavenConfiguration configuration)
            throws MalformedURLException {
        super(url);
        NullArgumentException.validateNotNull(url, "URL cannot be null");
        NullArgumentException.validateNotNull(configuration, "Service configuration");
        m_parser = new Parser(url.getPath());

          ArrayList<String> r = new ArrayList<String>();

        for (MavenRepositoryURL s : configuration.getRepositories()) {
            if (!s.isFileRepository()) {
                r.add(s.getURL().toExternalForm());
            }
        }

        //String[] repos = "http://repo1.maven.org/maven2/".split(",");

        String local = configuration.getLocalRepository().getFile().getAbsolutePath();

         m_aetherBasedResolver = new AetherBasedResolver(local, r.toArray(new String[r.size()]));
    }

    /**
     * Does nothing.
     *
     * @see java.net.URLConnection#connect()
     */
    @Override
    public void connect() {
        // do nothing
    }

    /**
    * TODO doc
     */
    @Override
    public InputStream getInputStream()
            throws IOException {
        connect();
        
        LOG.debug("Resolving [" + url.toExternalForm() + "]");

        return m_aetherBasedResolver.resolve(m_parser.getGroup(), m_parser.getArtifact(), m_parser.getType(), m_parser.getVersion());
    }


}
