/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.url.maven.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;

/**
 * An URLConnextion that supports maven: protocol.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.0, March 22, 2009
 */
public class Connection
    extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Connection.class );
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
     * @throws java.net.MalformedURLException in case of a malformed url
     */
    public Connection( final URL url, final MavenConfiguration configuration )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( configuration, "Service configuration" );
        m_configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect()
    {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        LOG.debug( "Resolving [" + url.toExternalForm() + "]" );

        // no artifact found
        throw new RuntimeException(
            "URL [" + url.toExternalForm() + "] could not be resolved. (enable TRACE logging for details)"
        );
    }

}