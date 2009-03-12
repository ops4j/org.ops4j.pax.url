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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.ops4j.pax.url.bnd.BndUtils;
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
        final Properties instructions = BndUtils.parseInstructions( getURL().getQuery() );
        // war file to be processed
        instructions.setProperty( ServiceConstants.INSTR_WAR_URL, getURL().getPath() );
        // default import packages
        if( !instructions.containsKey( "Import-Package" ) )
        {
            instructions.setProperty(
                "Import-Package",
                "javax.*; resolution:=optional,"
                + "org.xml.*; resolution:=optional,"
                + "org.w3c.*; resolution:=optional"
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

}