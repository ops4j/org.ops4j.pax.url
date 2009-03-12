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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.ops4j.net.URLUtils;

/**
 * Url connection for war-file protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class WarReferenceConnection
    extends AbstractConnection
{

    /**
     * @see AbstractConnection#AbstractConnection(URL, Configuration)
     */
    public WarReferenceConnection( final URL url,
                               final Configuration config )
        throws MalformedURLException
    {
        super( url, config );
    }

    /**
     * Url must be a reference to an instructions file.
     *
     * @see AbstractConnection#getInstructions()
     */
    protected Properties getInstructions()
        throws IOException
    {
        final Properties instructions = new Properties();
        final URL instructionsFleUrl = getInstructionsFileURL();
        instructions.load(
            URLUtils.prepareInputStream( instructionsFleUrl, getConfiguration().getCertificateCheck() )
        );
        //the following line is for debugging purposes 
        //instructions.store( System.out, null );
        return instructions;
    }

    /**
     * Get the instructions file URL out of processed URL by trying first a direct url and if fails looking for a file
     * with the specified path.
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


}