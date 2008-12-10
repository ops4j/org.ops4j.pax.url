package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import org.ops4j.lang.NullArgumentException;

/**
 *
 * Expects a url like dir:file:///Users/foo/myroot
 * @author Toni Menzel (tonit)
 * @since Dec 10, 2008
 */
public class Parser
{

    File m_directory;

    Properties instructions = new Properties();

    public Parser( String url )
    {
        NullArgumentException.validateNotNull( url, "url should be provided" );
        try
        {
            URL u = new URL( url );
            URL path = new URL(u.getPath());
           
            // TODO: add more parsing stuff based on expression
            m_directory = new File( path.getPath() );

            verifyDirectory();
        } catch( MalformedURLException e )
        {
            throw new IllegalArgumentException( "path is not nice.", e );
        }
    }

    private void verifyDirectory()
    {
        NullArgumentException.validateNotNull( m_directory, "path should be a valid file" );
        try
        {
            if( !m_directory.exists() )
            {

                throw new IllegalArgumentException(
                    "Folder " + m_directory.getCanonicalPath() + " does not exist on local filesystem"
                );
            }
            if( !m_directory.isDirectory() )
            {

                throw new IllegalArgumentException(
                    "Path " + m_directory.getCanonicalPath() + " does not refer to a folder"
                );
            }
        } catch( IOException ioE )
        {
            throw new IllegalArgumentException( m_directory.getAbsolutePath() + " is not valid: ", ioE );
        }
    }

    public File getDirectory()
    {
        return m_directory;
    }
}
