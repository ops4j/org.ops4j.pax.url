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

import org.ops4j.pax.swissbox.bnd.BndUtils;
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

    private static final String JAVAX_SERVLET_HTTP = "javax.servlet.http";
	private static final String JAVAX_SERVLET = "javax.servlet";

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
        final Properties instructions = BndUtils.parseInstructions(getURL().getQuery());
        // war file to be processed
        instructions.setProperty( ServiceConstants.INSTR_WAR_URL, getURL().getPath() );
        // default import packages
        if( !instructions.containsKey( "Import-Package" ) ) //TODO: merge the provided packages with the required ones.
        {
            String packages = "javax.servlet,"
                + "javax.servlet.http,"
                + "javax.*; resolution:=optional,"
                + "org.xml.*; resolution:=optional,"
                + "org.w3c.*; resolution:=optional";
            instructions.setProperty(
                "Import-Package",
                packages
            );
        } else {
        	// Certain Packages are always needed and therefore should be appended.
        	String importPackages = instructions.getProperty("Import-Package");
        	
        	if ((importPackages.contains(JAVAX_SERVLET) )|| (importPackages.contains(JAVAX_SERVLET_HTTP) )) { 
        		//found javax.servlet
        		//check if javax.servlet or javax.servlet.http is contained.
        		boolean servletFound = false;
        		boolean servletHttpFound = false;
        		String[] imports = importPackages.split(",");
        		for (String importstmt : imports) {
        			if (importstmt.contains(JAVAX_SERVLET)) {
        				if (importstmt.length() > JAVAX_SERVLET.length() && importstmt.charAt(JAVAX_SERVLET.length()) == '.') {
	        					//we found a javax.servlet.
	        					//check if it is a javax.servlet.http
	        					if (importstmt.contains(JAVAX_SERVLET_HTTP))
	        						servletHttpFound = true;
        				} else {
        					servletFound = true;
        				}
        			}
				}
        		if (!servletFound) {
        			importPackages += ","+JAVAX_SERVLET;
        		}
        		if (!servletHttpFound) {
        			importPackages += ","+JAVAX_SERVLET_HTTP;
        		}
        	} else { //both are missing
        		importPackages += ","+JAVAX_SERVLET+","+JAVAX_SERVLET_HTTP;
        	}
        	
        	instructions.setProperty("Import-Package", importPackages);
        }
        
        if( getConfiguration().getImportPaxLoggingPackages() )
        {
        	String importPackages = instructions.getProperty("Import-Package");
            String provider = ";provider=paxlogging;resolution:=optional";
            importPackages +=
                    ",org.apache.commons.logging" + provider +
                    ",org.apache.commons.logging.impl" + provider +
                    ",org.apache.log4j" + provider +
                    ",org.apache.log4j.spi" + provider +
                    ",org.apache.log4j.xml" + provider +
                    ",org.slf4j" + provider +
                    ",org.slf4j.helpers" + provider +
                    ",org.slf4j.spi" + provider;
            
            instructions.setProperty("Import-Package", importPackages);
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