package org.ops4j.pax.url.dir.workspace;

import java.io.File;
import java.io.IOException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 7, 2009
 */
public class WorkspaceFactory
{

    /**
     * It locates source and compiled location assumes its common root being the project root folder.
     *
     * @param sample a sample class of the project you find to find the root for.
     */
    public static Workspace getWorkspaceByClass( Class sample )
        throws IOException
    {
        String pathPrefix = sample.getName().replaceAll( "\\.", "/" );

        File p1 = new FileTailImpl( new File( "." ), pathPrefix + ".class" ).getParentOfTail();
        File p2 = new FileTailImpl( new File( "." ), pathPrefix + ".java" ).getParentOfTail();
        Workspace w = new Workspace( getCrossPoint( p1, p2 ) );

        return w;
    }

    private static File getCrossPoint( File p1, File p2 )
        throws IOException
    {
        int i = 0;
        for( char c : p1.getCanonicalPath().toCharArray() )
        {
            if( c != p2.getCanonicalPath().charAt( i++ ) )
            {
                return new File( p1.getCanonicalPath().substring( 0, i - 1 ) );
            }
        }

        return null;
    }
}
