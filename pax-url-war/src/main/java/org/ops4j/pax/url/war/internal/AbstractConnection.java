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
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.pax.url.war.ServiceConstants;
import org.ops4j.pax.url.wrap.internal.bnd.BndLib;

/**
 * Abstract url connection for wrap protocol handler.
 * Subclasses must provide the processing instructions.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
abstract class AbstractConnection
    extends URLConnection
{

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
     * @throws MalformedURLException if url path is empty
     * @throws NullArgumentException if url or configuration is null
     */
    protected AbstractConnection( final URL url,
                               final Configuration configuration )
        throws MalformedURLException
    {
        super( url );

        NullArgumentException.validateNotNull( url, "URL" );
        NullArgumentException.validateNotNull( configuration, "Configuration" );

        final String path = url.getPath();
        if( path == null || path.trim().length() == 0 )
        {
            throw new MalformedURLException( "Path cannot empty" );
        }

        m_configuration = configuration;
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
        final Properties instructions = getInstructions();
        PreConditionException.validateNotNull( instructions, "Instructions" );

        // the instructions must always contain the war file
        final String warUri = instructions.getProperty( ServiceConstants.INSTR_WAR_URI );
        if( warUri == null || warUri.trim().length() == 0 )
        {
            throw new IOException(
                "Instructions file must contain a property named " + ServiceConstants.INSTR_WAR_URI
            );
        }

        generateClassPathInstruction( instructions );

        return BndLib.createBundle(
            new URL( warUri ).openStream(),
            instructions,
            warUri
        );

    }

    /**
     * Returns the processing instructions.
     *
     * @return processing instructions
     *
     * @throws java.io.IOException if instructions file can not be returned
     */
    protected abstract Properties getInstructions()
        throws IOException;

    /**
     * Getter.
     *
     * @return configuration
     */
    protected Configuration getConfiguration()
    {
        return m_configuration;
    }

    /**
     * Generates the Bundle-ClassPath header by merging the Original classpath with:<br/>
     * .<br/>
     * WEB-INF/classes<br/>
     * all jars found in WEB-INF/lib
     *
     * @param instructions instructions
     *
     * @throws java.io.IOException re-thrown from extractJarListFromWar()
     */
    private static void generateClassPathInstruction( final Properties instructions )
        throws IOException
    {
        final List<String> bundleClassPath = new ArrayList<String>();
        // first take the bundle class path if present
        bundleClassPath.addAll( toList( instructions.getProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH ), "," ) );
        // then get the list of jars in WEB-INF/lib
        bundleClassPath.addAll( extractJarListFromWar( instructions.getProperty( ServiceConstants.INSTR_WAR_URI ) ) );
        // check if we have a "WEB-INF/classpath" entry
        if( !bundleClassPath.contains( "WEB-INF/classes" ) )
        {
            bundleClassPath.add( 0, "WEB-INF/classes" );
        }
        // check if we have a "." entry
        if( !bundleClassPath.contains( "." ) )
        {
            bundleClassPath.add( 0, "." );
        }
        // set back the new bundle classpath
        instructions.setProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH, join( bundleClassPath, "," ) );
    }

    /**
     * Does nothing.
     */
    @Override
    public void connect()
    {
        // do nothing
    }

    /**
     * Extracts the list of jars from a war file. The list will contain all jars under WEB-INF/lib directory.
     *
     * @param warUri war file uri
     *
     * @return list of jars
     *
     * @throws java.io.IOException re-thrown from accessing urls or if the warUri does not refer to a jar
     */
    private static List<String> extractJarListFromWar( final String warUri )
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

    /**
     * Splits a delimiter separated string into a list.
     *
     * @param separatedString string to be split
     * @param delimiter       delimiter
     *
     * @return list composed out of the string segments
     */
    private static List<String> toList( final String separatedString, final String delimiter )
    {
        final List<String> list = new ArrayList<String>();
        if( separatedString != null )
        {
            list.addAll( Arrays.asList( separatedString.split( delimiter ) ) );
        }
        return list;
    }

    /**
     * Joins elements from a collection into a delimiter separated string.
     *
     * @param strings   collection of ellements
     * @param delimiter delimiter
     *
     * @return string composed from the collection elements delimited by the delimiter
     */
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

}