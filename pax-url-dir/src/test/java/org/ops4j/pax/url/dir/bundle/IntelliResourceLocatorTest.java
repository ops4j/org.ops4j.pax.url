package org.ops4j.pax.url.dir.bundle;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.junit.Test;
import static junit.framework.Assert.*;
import org.ops4j.pax.url.dir.bundle.ResourceLocator;
import org.ops4j.pax.url.dir.internal.NullOutputStream;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 11, 2008
 */
public class IntelliResourceLocatorTest
{

    @Test( expected = IllegalArgumentException.class )
    public void simpleFailingAtConstruct()
        throws IOException
    {
        ResourceLocator loc = new ResourceLocator( null,"" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void simpleFailingAtWriteToNull()
        throws IOException
    {
        ResourceLocator loc = new ResourceLocator( new File( "." ), "" );
        loc.write( null );
    }

    @Test
    public void testRoot()
        throws IOException
    {
        String clazz = this.getClass().getName().replaceAll( "\\.", "/" ) + ".class";
        ResourceLocator loc = new ResourceLocator( new File( "." ), clazz );
        final int[] countOfEntries = new int[]{ 0 };

        JarOutputStream out = new JarOutputStream( new NullOutputStream() )
        {

            @Override
            public void putNextEntry( ZipEntry zipEntry )
                throws IOException
            {
                super.putNextEntry( zipEntry );
                countOfEntries[ 0 ]++;
            }


        };
        assertEquals( "test-classes", loc.getRoot().getName() );
        loc.write( out );

        assertEquals( 8, countOfEntries[ 0 ] );
    }
}
