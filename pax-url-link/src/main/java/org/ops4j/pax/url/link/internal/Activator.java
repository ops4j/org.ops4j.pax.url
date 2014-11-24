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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.link.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for "link:" protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, March 10, 2009
 */
public final class Activator
    extends HandlerActivator<Void>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<Void>()
            {

                /**
                 * Creates a "link" url connection.
                 *
                 * @see ConnectionFactory#createConnection(BundleContext, URL, Object)
                 */
                public URLConnection createConnection( final BundleContext bundleContext,
                                                       final URL url,
                                                       final Void notUsed )
                    throws IOException
                {
                    return Activator.createConnection( url );
                }

                /**
                 * @see ConnectionFactory#createConfiguration(PropertyResolver)
                 */
                public Void createConfiguration( final PropertyResolver propertyResolver )
                {
                    return null;
                }

            }
        );
    }

    /**
     * Creates a "link" url connection.
     *
     * @param url link file url
     *
     * @throws java.io.IOException - If occured during reading the link file
     *                             - If linked url could not be found in the link file
     *                             - If linked url cannot be accessed
     */
    public static URLConnection createConnection( final URL url )
        throws IOException
    {
        final String[] content = readLinkFile( new Parser( url.getPath() ).getUrl() );
        for( final String line : content )
        {
            if( line == null
                || line.trim().length() == 0
                || line.trim().startsWith( "#" ) )
            {
                continue;
            }
            return new URL( line ).openConnection();
        }
        throw new IOException( "Linked file could not be parsed into an URL" );
    }

    /**
     * Read a file and return the list of lines in an array of strings.
     *
     * @param listFile the url to read from
     *
     * @return the lines
     *
     * @throws java.io.IOException if a read error occurs
     */
    private static String[] readLinkFile( final URL listFile )
        throws IOException
    {
        ArrayList<String> list = new ArrayList<String>();
        InputStream stream = listFile.openStream();
        try
        {
            InputStreamReader isr = new InputStreamReader( stream, "UTF-8" );
            BufferedReader reader = new BufferedReader( isr );
            String line = reader.readLine();
            while( line != null )
            {
                list.add( line );
                line = reader.readLine();
            }
            String[] items = new String[list.size()];
            list.toArray( items );
            return items;
        }
        finally
        {
            stream.close();
        }
    }

}