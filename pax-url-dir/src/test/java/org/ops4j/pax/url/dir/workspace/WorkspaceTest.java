package org.ops4j.pax.url.dir.workspace;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 7, 2009
 */
public class WorkspaceTest
{

    @Test
    public void simpleTest()
        throws IOException
    {
        Workspace workspace = WorkspaceFactory.getWorkspaceByClass( WorkspaceTest.class );
        assertNotNull( workspace.getDirectory() );
        assertNotNull( workspace.getPom() );
    }
}
