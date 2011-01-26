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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ops4j.pax.url.war.ServiceConstants;

/**
 * Url connection for war protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class WarConnection
    extends AbstractConnection
{

    /**
     * @see AbstractConnection#AbstractConnection(URL, Configuration)
     */
    public WarConnection( final URL url,
                          final Configuration config )
        throws MalformedURLException
    {
        super( url, config );
    }

    /**
     * Creates a set of default instructions.
     *
     * @see AbstractConnection#getInstructions()
     */
    protected Properties getInstructions()
        throws MalformedURLException
    {
        final Properties instructions = parseInstructions( getURL().getQuery() );
        // war file to be processed
        instructions.setProperty( ServiceConstants.INSTR_WAR_URL, getURL().getPath() );
        // default import packages
        if( !instructions.containsKey( "Import-Package" ) )
        {
            String packages = "javax.servlet,"
                + "javax.servlet.http,"
                + "javax.*; resolution:=optional,"
                + "org.xml.*; resolution:=optional,"
                + "org.w3c.*; resolution:=optional";
            if( getConfiguration().getImportPaxLoggingPackages() )
            {
                String provider = ";provider=paxlogging;resolution:=optional";
                packages +=
                        ",org.apache.commons.logging" + provider +
                        ",org.apache.commons.logging.impl" + provider +
                        ",org.apache.log4j" + provider +
                        ",org.apache.log4j.spi" + provider +
                        ",org.apache.log4j.xml" + provider +
                        ",org.slf4j" + provider +
                        ",org.slf4j.helpers" + provider +
                        ",org.slf4j.spi" + provider;
            }
            instructions.setProperty(
                "Import-Package",
                packages
            );
        }
        // default no export packages
        if( !instructions.containsKey( "Export-Package" ) )
        {
            instructions.setProperty(
                "Export-Package",
                "!*"
            );
        }
        // remove unnecessary headers
        if( !instructions.containsKey( "-removeheaders" ) )
        {
            instructions.setProperty(
                "-removeheaders",
                "Private-Package,"
                + "Ignore-Package"
            );
        }
        return instructions;
    }

    /**
     * Regex pattern for matching instructions when specified in url.
     */
    private static final Pattern INSTRUCTIONS_PATTERN =
        Pattern.compile("([a-zA-Z_0-9-]+)=([ \\-!\"'()\\[\\]*+,.0-9A-Z_a-z%;:=/]+)");


    /**
     * Parses bnd instructions out of an url query string.
     *
     * @param query query part of an url.
     *
     * @return parsed instructions as properties
     *
     * @throws java.net.MalformedURLException if provided path does not comply to syntax.
     */
    public static Properties parseInstructions( final String query )
        throws MalformedURLException
    {
        final Properties instructions = new Properties();
        if( query != null )
        {
            try
            {
                // just ignore for the moment and try out if we have valid properties separated by "&"
                final String segments[] = query.split( "&" );
                for( String segment : segments )
                {
                    // do not parse empty strings
                    if( segment.trim().length() > 0 )
                    {
                        final Matcher matcher = INSTRUCTIONS_PATTERN.matcher( segment );
                        if( matcher.matches() )
                        {
                            instructions.setProperty(
                                matcher.group( 1 ),
                                URLDecoder.decode(matcher.group(2), "UTF-8")
                            );
                        }
                        else
                        {
                            throw new MalformedURLException( "Invalid syntax for instruction [" + segment
                                                             + "]. Take a look at http://www.aqute.biz/Code/Bnd."
                            );
                        }
                    }
                }
            }
            catch( UnsupportedEncodingException e )
            {
                // thrown by URLDecoder but it should never happen
                throw (MalformedURLException) new MalformedURLException( "Could not retrieve the instructions from [" + query + "]").initCause( e );
            }
        }
        return instructions;
    }

}