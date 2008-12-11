package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.ops4j.lang.NullArgumentException;
import java.util.StringTokenizer;

/**
 * Expects a url like dir:file:///Users/foo/myroot
 *
 * @author Toni Menzel (tonit)
 * @since Dec 10, 2008
 */
public class Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "dir:local-file-uri[$building-m_options]";

    private static final String OPTION_SEPARATOR = ",";

    private static final String ANCHOR = "anchor";

    /**
     * Separator between wrapped jar url and instructions.
     */
    private static final String INSTRUCTIONS_SEPARATOR = "$";

    /**
     * Regex pattern for matching jar and instructions.
     */
    private static final Pattern SYNTAX_JAR_INSTR =
        Pattern.compile( "(.+?)\\" + INSTRUCTIONS_SEPARATOR + "(.+?)" );

    private File m_directory;

    private Properties m_options = new Properties();
    private String m_marker;

    public Parser( String url )
    {
        NullArgumentException.validateNotNull( url, "url should be provided" );
        try
        {
            URL u = new URL( url );
            URL originalURL = new URL( u.getPath() );

            Matcher matcher = SYNTAX_JAR_INSTR.matcher( originalURL.getPath() );
            if( matcher.matches() )
            {
                // we have a local file uri and m_options
                m_directory = new File( matcher.group( 1 ) );
                parseOptions( matcher.group( 2 ) );
            }
            else
            {
                m_directory = new File( originalURL.getPath() );
            }
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

  

    private void parseOptions( String s )
    {
        StringTokenizer tk = new StringTokenizer( s, OPTION_SEPARATOR );
        while( tk.hasMoreTokens() )
        {
            String key = tk.nextToken( "=" );
            String value = tk.nextToken();
            m_options.put( key, value );
        }
    }

    public Properties getOptions()
    {
        return m_options;
    }

    public String getAnchor()
    {
        return (String) m_options.get( ANCHOR );
    }
}
