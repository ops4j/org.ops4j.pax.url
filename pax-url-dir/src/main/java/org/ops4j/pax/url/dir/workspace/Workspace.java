/*
 * Copyright 2009 Toni Menzel.
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
package org.ops4j.pax.url.dir.workspace;

import java.io.File;
import java.io.IOException;

/**
 * Utility class to use some functionality of this url hander as api.
 * Probably to be move to base project.
 *
 * @author Toni Menzel (tonit)
 * @since Mar 7, 2009
 */
public class Workspace
{

    private File m_directory;

    public Workspace( File projectRoot )
    {
        m_directory = projectRoot;
    }

    public File getDirectory()
    {
        return m_directory;
    }

    public File getPom()
        throws IOException
    {
        File f = new File( m_directory, "pom.xml" );
        if( f.exists() )
        {
            return f;
        }
        else
        {
            throw new IOException( "Folder " + m_directory + " is not a maven2 project root." );
        }
    }
}
