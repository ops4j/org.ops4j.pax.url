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
package org.ops4j.pax.url.cache.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.ops4j.io.StreamUtils;
import org.ops4j.lang.NullArgumentException;

/**
 * TODO Add JavaDoc.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 06 02, 2009
 */
public class Connection
    extends URLConnection
{

    /**
     * Parsed url.
     */
    private final Parser m_parser;
    /**
     * Service configuration.
     */
    private final Configuration m_configuration;
    /**
     * Name corresponding to url (base64 encoding).
     */
    private final String m_cacheName;
    /**
     * Url property name.
     */
    private static final String META_URL = "url";
    /**
     * Cached time property name.
     */
    private static final String META_CACHED_ON = "cachedOn";
    /**
     * Meta file extension.
     */
    private static final String EXT_META = ".meta";
    /**
     * Data file extension.
     */
    private static final String EXT_DATA = ".data";

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

        NullArgumentException.validateNotNull( url, "URL" );
        NullArgumentException.validateNotNull( configuration, "Configuration" );

        m_parser = new Parser( url.getPath() );
        m_configuration = configuration;
        m_cacheName = generateCacheName( m_parser.getUrl() );
    }

    /**
     * Generate caching name out of url.
     *
     * @param url url to be cached
     *
     * @return cache name
     *
     * @throws IOException - If cache name cannot be generated
     */
    private static String generateCacheName( final URL url )
        throws IOException
    {
        final MessageDigest md5;
        try
        {
            md5 = MessageDigest.getInstance( "MD5" );
        }
        catch( NoSuchAlgorithmException e )
        {
            throw new IOException( "Cannot generate caching name (MD5 not supported)" );
        }
        String encoded = new BigInteger( 1, md5.digest( url.toExternalForm().getBytes() ) ).toString( 16 );
        if( encoded.length() == 31 )
        {
            encoded = "0" + encoded;
        }
        return encoded;
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
     * @throws java.io.IOException in case of an exception during accessing the resource
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        final File workingDir = m_configuration.getWorkingDirectory();
        workingDir.mkdirs();
        final File cacheMetaFile = new File( workingDir, m_cacheName + EXT_META );
        final File cacheDateFile = new File( workingDir, m_cacheName + EXT_DATA );

        final Properties cacheMeta = new Properties();
        try
        {
            InputStream in = new FileInputStream( cacheMetaFile );
            try
            {
                cacheMeta.load( in );
            }
            finally
            {
                in.close();
            }
        }
        catch( FileNotFoundException ignore )
        {
            //ignore 
        }

        final String cacheUrl = cacheMeta.getProperty( META_URL );
        if( cacheUrl == null )
        {
            cacheMeta.setProperty( META_URL, url.getPath() );
        }

        final String cacheTime = cacheMeta.getProperty( META_CACHED_ON );
        if( cacheTime == null || !cacheDateFile.exists() )
        {
            StreamUtils.copyStream(
                m_parser.getUrl().openStream(),
                new BufferedOutputStream( new FileOutputStream( cacheDateFile ) ),
                true
            );
            cacheMeta.setProperty( META_CACHED_ON, String.valueOf( System.currentTimeMillis() ) );
        }

        OutputStream out = new FileOutputStream( cacheMetaFile );
        try
        {
            cacheMeta.store( out, null );
        }
        finally
        {
            out.close();
        }
        return new BufferedInputStream( new FileInputStream( cacheDateFile ) );
    }
}


