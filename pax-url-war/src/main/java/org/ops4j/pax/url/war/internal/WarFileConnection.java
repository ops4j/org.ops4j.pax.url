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
package org.ops4j.pax.url.war.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.war.ServiceConstants;

/**
 * An URLConnection that implements warfile protocol.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class WarFileConnection
    extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WarConnection.class );
    /**
     * Memory repository.
     */
    private final MemoryRepository m_memoryRepository;

    /**
     * Creates a new connection.
     *
     * @param url           the url; cannot be null.
     * @param configuration service configuration; cannot be null
     *
     * @throws java.net.MalformedURLException in case of a malformed url
     */
    public WarFileConnection( final URL url, final MemoryRepository memoryRepository )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( memoryRepository, "Memory repository" );
        final String path = getURL().getPath();
        if( path == null || path.trim().length() == 0 )
        {
            throw new MalformedURLException( "War file URL must be specified" );
        }
        m_memoryRepository = memoryRepository;
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
     * Returns the input stream denoted by the url.
     *
     * @return the input stream for the resource denoted by url
     *
     * @throws java.io.IOException in case of an exception during accessing the resource
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        // create a property file with the war file uri
        final Properties properties = new Properties();
        properties.setProperty( ServiceConstants.INSTR_WAR_URI, getURL().getPath() );
        // default import packages
        properties.setProperty(
            "Import-Package",
            "javax.*; resolution:=optional,"
            + "org.xml.*; resolution:=optional,"
            + "org.w3c.*; resolution:=optional"
        );
        // default no export packages
        properties.setProperty(
            "Export-Package",
            "!*"
        );
        // and store it into memory
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store( baos, null );
        final MemoryRepository.Reference reference = m_memoryRepository.add( baos.toByteArray() );
        // we have to wrap the input stream so we can remove the wrapping instructions file from memory
        return new MemoryInputStream(
            new URL( ServiceConstants.PROTOCOL_WAR + ":" + WarMemConnection.toExternalForm( reference ) ).openStream(),
            reference
        );
    }

}