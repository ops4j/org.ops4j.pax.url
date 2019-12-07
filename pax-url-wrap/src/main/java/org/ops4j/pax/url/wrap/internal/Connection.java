/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2007 Peter Kriens.  
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
package org.ops4j.pax.url.wrap.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.swissbox.bnd.BndUtils;

/**
 * Url connection for wrap protocol handler.
 *
 * @author Alin Dreghiciu
 * @since September 09, 2007
 */
public class Connection
    extends URLConnection
{

    /**
     * Parsed url.
     */
    private Parser m_parser;
    /**
     * Service configuration.
     */
    private final Configuration m_configuration;

    /**
     * Creates a new connection.
     *
     * @param url           the url; cannot be null.
     * @param configuration service configuration; cannot be null
     *
     * @throws MalformedURLException in case of a malformed url
     */
    public Connection( final URL url, final Configuration configuration )
        throws MalformedURLException
    {
        super( url );

        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( configuration, "Service configuration" );

        m_configuration = configuration;
        m_parser = new Parser( url.getPath(), m_configuration.getCertificateCheck() );
    }

    /**
     * Returns an input stream for the bundle created from the jar.
     *
     * @return the input stream for the bundle created from the jar
     *
     * @throws IOException re-thrown from BndLib.createBundle
     * @see BndUtils#createBundle(InputStream, Properties, String)
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        return BndUtils.createBundle(
            URLUtils.prepareInputStream(
                m_parser.getWrappedJarURL(),
                !m_configuration.getCertificateCheck()
            ),
            m_parser.getWrappingProperties(),
            url.toExternalForm(),
            m_parser.getOverwriteMode()
        );
    }

    /**
     * Does nothing.
     */
    @Override
    public void connect()
    {
        // do nothing
    }

}
