/*
 * Copyright 2009 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.manifest.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.ops4j.lang.NullArgumentException;
import static org.ops4j.lang.NullArgumentException.*;

/**
 * "manifest:" protocol connection.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 1.1.0, June 24, 2009
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
     * @param url           url to be handled; cannot be null.
     * @param configuration protocol configuration; cannot be null
     *
     * @throws MalformedURLException - If url path is empty
     * @throws IOException           - If cache name cannot be generated
     * @throws NullArgumentException - If url or configuration is null
     */
    protected Connection( final URL url,
                          final Configuration configuration )
        throws IOException
    {
        super( url );

        validateNotNull( url, "URL" );
        validateNotNull( configuration, "Configuration" );

        m_parser = new Parser( url.getPath() );
        m_configuration = configuration;
    }

    /**
     * Does nothing.
     */
    @Override
    public void connect()
    {
        //do nothing
    }

    /**
     * Returns the input stream denoted by the url.
     *
     * @return the input stream for the resource denoted by url
     *
     * @throws IOException in case of an exception during accessing the resource
     * @see URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        throw new UnsupportedOperationException();
    }
}