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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.runner.commons.url.URLUtils;
import org.ops4j.pax.url.war.ServiceConstants;

/**
 * An URLConnection that implements war protocol.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class WarConnection
    extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WarConnection.class );
    /**
     * Service configuration.
     */
    private final Configuration m_configuration;
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
    public WarConnection( final URL url, final Configuration configuration, final MemoryRepository memoryRepository )
        throws MalformedURLException
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( configuration, "Service configuration" );
        NullArgumentException.validateNotNull( memoryRepository, "Memory repository" );
        m_configuration = configuration;
        m_memoryRepository = memoryRepository;
    }

    /**
     * Does nothing.
     *
     * @see URLConnection#connect()
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
     * @see URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        final Properties properties = new Properties();
        final URL instructionsFleUri = getInstructionsFileURL();
        properties.load(
            URLUtils.prepareInputStream( instructionsFleUri, m_configuration.getCertificateCheck() )
        );
        // the properties must always contain the war file
        final String warUri = properties.getProperty( ServiceConstants.INSTR_WAR_URI );
        if( warUri == null || warUri.trim().length() == 0 )
        {
            throw new IOException(
                "Instructions file must contain a property named " + ServiceConstants.INSTR_WAR_URI
            );
        }
        final List<String> bundleClassPath = new ArrayList<String>();
        // first take the bundle class path if present
        bundleClassPath.addAll( toList( properties.getProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH ), "," ) );
        // then get the list of jars in WEB-INF/lib
        bundleClassPath.addAll( extractJarListFromWar( warUri ) );
        // check if we have a "WEB-INF/classpath" entry
        if( !bundleClassPath.contains( "WEB-INF/classpath" ) )
        {
            bundleClassPath.add( 0, "WEB-INF/classpath" );
        }
        // check if we have a "." entry
        if( !bundleClassPath.contains( "." ) )
        {
            bundleClassPath.add( 0, "." );
        }
        // set back the new bundle classpath
        properties.setProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH, join( bundleClassPath, "," ) );
        // add instructions file to memory repository
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store( baos, null );
        final MemoryRepository.Reference reference = m_memoryRepository.add( baos.toByteArray() );
        // we have to wrap the input stream so we can remove the wrapping instructions file from memory
        return new MemoryInputStream(
            new URL( "wrap:" + warUri + "," + WarMemConnection.toExternalForm( reference ) ).openStream(),
            reference
        );
    }

    /**
     * Extracts the list of jars from a war file. The list will contain all jars under WEB-INF/lib directory.
     *
     * @param warUri war file uri
     *
     * @return list of jars
     */
    private List<String> extractJarListFromWar( final String warUri )
        throws IOException
    {
        final List<String> list = new ArrayList<String>();
        JarFile jarFile = null;
        try
        {
            final JarURLConnection conn = (JarURLConnection) new URL( "jar:" + warUri + "!/" ).openConnection();
            conn.setUseCaches( false );
            jarFile = conn.getJarFile();
            Enumeration entries = jarFile.entries();
            while( entries.hasMoreElements() )
            {
                JarEntry entry = (JarEntry) entries.nextElement();
                String name = entry.getName();
                if( !name.startsWith( "WEB-INF/lib/" ) )
                {
                    continue;
                }
                if( !name.endsWith( ".jar" ) )
                {
                    continue;
                }
                list.add( name );
            }
        }
        catch( ClassCastException e )
        {
            throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file" );
        }
        finally
        {
            if( jarFile != null )
            {
                try
                {
                    jarFile.close();
                }
                catch( IOException ignore )
                {
                    // ignore
                }
            }
        }
        return list;
    }

    private static List<String> toList( final String separatedString, final String delimiter )
    {
        final List<String> list = new ArrayList<String>();
        if( separatedString != null )
        {
            list.addAll( Arrays.asList( separatedString.split( delimiter ) ) );
        }
        return list;
    }

    private static String join( final Collection<String> strings, final String delimiter )
    {
        final StringBuffer buffer = new StringBuffer();
        final Iterator<String> iter = strings.iterator();
        while( iter.hasNext() )
        {
            buffer.append( iter.next() );
            if( iter.hasNext() )
            {
                buffer.append( delimiter );
            }
        }
        return buffer.toString();
    }

    /**
     * Get the instructions file URL out of processed URL.
     *
     * @return instructions file URL out of processed URL.
     *
     * @throws IOException re-thrown
     */
    private URL getInstructionsFileURL()
        throws IOException
    {
        // first try an url out of the path
        try
        {
            return new URL( getURL().getPath() );
        }
        catch( MalformedURLException e )
        {
            // give one more try to file
            final File instructionsFile = new File( getURL().getPath() );
            if( instructionsFile.exists() && instructionsFile.isFile() )
            {
                return instructionsFile.toURL();
            }
            throw e;
        }
    }

    /**
     * Creates an IOException with a message and a cause.
     *
     * @param message exception message
     * @param cause   exception cause
     *
     * @return the created IO Exception
     */
    private static IOException initIOException( final String message, final Exception cause )
    {
        IOException exception = new IOException( message );
        exception.initCause( cause );
        return exception;
    }

}