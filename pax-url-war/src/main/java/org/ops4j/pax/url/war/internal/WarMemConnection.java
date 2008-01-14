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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.war.ServiceConstants;

/**
 * An URLConnection that implements warmem protocol.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class WarMemConnection
    extends URLConnection
{

    /**
     * Memory repository.
     */
    private final MemoryRepository m_memoryRepository;
    /**
     * Referenced memory id.
     */
    private final String m_id;
    /**
     * Memory content. Valid only after connect.
     */
    private byte[] m_byteArray;

    /**
     * Creates a new connection.
     *
     * @param url              the url; cannot be null.
     * @param memoryRepository the memory repository that backs up the connection
     *
     * @throws java.net.MalformedURLException in case that the path does not contain a non empty string = id
     */
    public WarMemConnection( final URL url, final MemoryRepository memoryRepository )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( memoryRepository, "Memory repository" );
        m_id = getURL().getPath();
        if( m_id == null || m_id.trim().length() == 0 )
        {
            throw new MalformedURLException();
        }
        m_memoryRepository = memoryRepository;
    }

    /**
     * Access the memory repository.
     *
     * @throws IOException if memory repository does not contain a myte array for the id.
     * @see java.net.URLConnection#connect()
     */
    @Override
    public void connect()
        throws IOException
    {
        if( m_byteArray == null )
        {
            m_byteArray = m_memoryRepository.get( m_id );
            if( m_byteArray == null )
            {
                throw new IOException( "Invalid reference" );
            }
        }
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
        final InputStream is = new ByteArrayInputStream( m_byteArray );
        // release the byte array
        m_byteArray = null;
        return is;
    }

    public static String toExternalForm( final MemoryRepository.Reference reference )
    {
        NullArgumentException.validateNotNull( reference, "Memory reference" );
        return ServiceConstants.PROTOCOL_WAR_MEM + ":" + reference.getId();
    }

}