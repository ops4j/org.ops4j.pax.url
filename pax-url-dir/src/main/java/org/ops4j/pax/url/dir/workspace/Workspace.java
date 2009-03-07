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
