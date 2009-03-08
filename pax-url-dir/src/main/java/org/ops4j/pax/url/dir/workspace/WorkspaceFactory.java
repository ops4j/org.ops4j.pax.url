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
