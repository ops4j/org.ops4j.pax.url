package org.ops4j.pax.url.dir.internal.bundle;

import java.io.File;
import java.io.IOException;
import static junit.framework.Assert.*;
import org.junit.Test;
import org.ops4j.pax.url.dir.internal.FileTailImpl;
import org.ops4j.pax.url.dir.internal.FileTail;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 11, 2008
 */
public class FileTailTest
{

    @Test( expected = IllegalArgumentException.class )
    public void simpleFailingAtConstruct()
        throws IOException
    {
        FileTail loc = new FileTailImpl( null, "" );
    }

    @Test
    public void testRoot()
        throws IOException
    {
        String clazz = this.getClass().getName().replaceAll( "\\.", "/" ) + ".class";
        FileTail loc = new FileTailImpl( new File( "." ), clazz );
      
        
        assertEquals( "test-classes", loc.getParentOfTail().getName() );

    }
}
