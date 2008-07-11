package org.ops4j.pax.url.mvnlive.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * @author Toni Menzel (tonit)
 * @since Jul 10, 2008
 */
public class Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     * This accepts groupId/artifact that will mvnlive lead try finding the artifact in the base directory (and sub)
     *
     * the local_uri should point to a local directory
     */
    private static final String SYNTAX = "mvnlive:groupId/artifactId | mvnlive:local_uri";
    private String m_path;

    public Parser( String path )
    {
        m_path = path;
    }

    /**
     * Will try to find the projectFolder specified in the path.
     *
     * @return the local projectfolder or null if it is invalid.
     */
    public File getProjectFolder()
        throws IOException
    {
        File res = null;
        try
        {
            // try to resolve as file path
            res = new File( new URI( m_path ).getPath() );

        } catch( Exception e )
        {
            // e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if( res == null || !res.canRead() )
        {
            // try to resolve as artifact name:
            String[] parts = m_path.split( "/" );
            // dig up until last pom
            File last = digUpPomPath( new File( "." ).getCanonicalFile() );
            // try to find it from current base
            res = find( last, parts[ 0 ], parts[ 1 ] );


        }
        return res;
    }

    private File digUpPomPath( File file )
        throws IOException
    {
        File deeper = new File( file.getParentFile().getCanonicalPath() + "/pom.xml" );

        if( deeper.exists() && deeper.canRead() )
        {
            return digUpPomPath( deeper.getParentFile() );
        }
        // return current file
        //    System.out.println( "Project base is " + file.getCanonicalPath() );
        return file;
    }

    /**
     * Walks down the sub directories
     *
     * @param curr
     * @param groupId
     * @param artifactId
     */
    private File find( File curr, String groupId, String artifactId )
    {
        // asume that we should have to walk down the base dir
        for( File c : curr.listFiles() )
        {
            if( c.isFile() && c.getName().equals( "pom.xml" ) )
            {
                if( isMatchingPom( c, groupId, artifactId ) )
                {
                    return c.getParentFile();
                }
            }
            else if( c.isDirectory() && !"src,target,site".contains( c.getName() ) )
            {
                File r = find( c, groupId, artifactId );
                if( r != null )
                {
                    return r;
                }
            }
        }

        return null;
    }

    // parses the pom
    private boolean isMatchingPom( File file, String groupId, String artifactId )
    {
        if( file.exists() && file.canRead() )
        {
            String foundGroup = null;
            String foundArtifact = null;
            try
            {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware( true );
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput( new FileInputStream( file ), null );
                // parse the flat pom stuff out
                int eventType = xpp.getEventType();
                while( eventType != XmlPullParser.END_DOCUMENT )
                {
                    if( eventType == XmlPullParser.START_TAG && xpp.getDepth() == 2 )
                    {
                        if( xpp.getName().equals( "groupId" ) )
                        {
                            xpp.next();
                            foundGroup = xpp.getText();
                        }
                        else if( xpp.getName().equals( "artifactId" ) )
                        {
                            xpp.next();
                            foundArtifact = xpp.getText();
                        }
                    }
                    eventType = xpp.next();
                }
            } catch( XmlPullParserException e )
            {
                e.printStackTrace();
            } catch( IOException e )
            {
                e.printStackTrace();
            }

            return ( ( groupId.equals( foundGroup ) && ( artifactId.equals( foundArtifact ) ) ) );
        }
        else
        {
            return false;
        }

    }


}
