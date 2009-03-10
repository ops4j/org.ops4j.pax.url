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
package org.ops4j.pax.url.link.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.ops4j.lang.NullArgumentException;

/**
 * An URLConnection that supports "link:" protocol.<br/>
 * Syntax:<br/>
 * link:<url>
 * where:<br/>
 * TODO
 * ...
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, March 10, 2009
 */
public class Connection
    extends URLConnection
{

    /**
     * URL path parser.
     */
    private URL m_referencedURL;
    /**
     * Service configuration.
     */
    private final Configuration m_configuration;

    /**
     * Creates a new connection.
     *
     * @param url the url; cannot be null
     *
     * @throws java.net.MalformedURLException - In case of a malformed url
     */
    public Connection( final URL url,
                       final Configuration configuration )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL" );
        NullArgumentException.validateNotNull( configuration, "Configuration" );
        
        m_referencedURL = new Parser( url.getPath() ).getReferencedUrl();
        m_configuration = configuration;
    }

    /**
     * Does nothing.
     * {@inheritDoc}
     */
    @Override
    public void connect()
    {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        throw new UnsupportedOperationException( "Not yet implemented" );
    }

}