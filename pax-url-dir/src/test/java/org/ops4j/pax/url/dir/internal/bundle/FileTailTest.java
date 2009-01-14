package org.ops4j.pax.url.dir.internal.bundle;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import static junit.framework.Assert.*;
import org.junit.Test;
import org.ops4j.pax.url.dir.internal.NullOutputStream;
import org.ops4j.pax.url.dir.internal.bundle.FileTail;

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
        FileTail loc = new FileTail( null, "" );
    }

    @Test
    public void testRoot()
        throws IOException
    {
        String clazz = this.getClass().getName().replaceAll( "\\.", "/" ) + ".class";
        FileTail loc = new FileTail( new File( "." ), clazz );
      
        
        assertEquals( "test-classes", loc.getParentOfTail().getName() );

    }
}
