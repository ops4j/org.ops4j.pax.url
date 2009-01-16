/*
 * Copyright 2008 Toni Menzel.
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
package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ops4j.lang.NullArgumentException;

/**
 * Expects a url like dir:/Users/foo/myroot
 *
 * New format suggestion:
 *
 * This is if you don't know the exact folders but examples of contents
 * dir:/Users/tonit/foo;org/ops4j/pax/Paxer.class&/Users/tonit/foo;org/ops4j/pax/PaxerTest.class$props=foo&other=bar
 *
 * If you want to just include certain files:
 * (give them a filename pattern)
 * dir:/Users/tonit/foo;org/ops4j/pax/Paxer.class;.*class&/Users/tonit/foo;org/ops4j/pax/PaxerTest.class$props=foo&other=bar
 *
 * If you know exact folders, this is the version without tail:
 * dir:/Users/tonit/foo/classes&/Users/tonit/foo/test-classes$props=foo&other=bar
 *
 * This would be the full format:
 * dir:<Dir>;<tail>;<filenamepattern>$<param>=<v>
 *
 * While tail and filenamepattern are optional.
 * 
 * On a higher level:
 * dir:<contentselection>$<paramters>
 *
 * paramters is optional
 * 
 * Where contentsection and parameters can be "chained" by & (like you see above in the samples)
 *
 * This supports multiple (different) roots, each one with teir own (optional) tail.
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

    private static final String OPTION_SEPARATOR = "&";

    private static final String TAIL = "tail";

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
            URL originalURL = u; //new URL( u.getPath() );
            Matcher matcher = SYNTAX_JAR_INSTR.matcher( originalURL.getPath() );
            if( matcher.matches() )
            {
                // we have a local file uri and m_options
                m_directory = toLocalFile( matcher.group( 1 )
                ); //new File( new URL( matcher.group( 1 ) ).getPath() ); // new File( matcher.group( 1 ) );

                parseOptions( matcher.group( 2 ) );
            }
            else
            {
                m_directory = toLocalFile( originalURL.getPath() );
                //  m_directory = new File( new URL( originalURL.getPath() ).getPath() );

            }
            verifyDirectory();
        } catch( IOException e )
        {
            throw new IllegalArgumentException( "path is not nice.", e );
        }
    }

    File toLocalFile( String s )
        throws IOException
    {
        // looks like file protocol cannot be relative, so we have to support local path instead of urls for now:
        return new File( s );
        // return new File( new URL( s ).getPath() );

    }

    private void verifyDirectory()
    {
        NullArgumentException.validateNotNull( m_directory, "path should be a valid file" );
        try
        {
            if( !m_directory.exists() )
            {

                throw new IllegalArgumentException(
                    "Folder " + m_directory.getAbsolutePath() + " does not exist on local filesystem"
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
            StringTokenizer inner = new StringTokenizer( tk.nextToken(), "=" );
            String key = inner.nextToken();
            String value = "";
            if( inner.hasMoreTokens() )
            {
                value = inner.nextToken();
            }
            m_options.put( key, value );
        }
    }

    public Properties getOptions()
    {
        return m_options;
    }

    public String getTailExpr()
    {
        return (String) m_options.get( TAIL );
    }
}
