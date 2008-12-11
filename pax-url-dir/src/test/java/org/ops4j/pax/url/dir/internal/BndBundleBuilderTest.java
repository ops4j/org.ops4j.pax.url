package org.ops4j.pax.url.dir.internal;

import java.util.Properties;
import java.util.ArrayList;
import java.util.jar.JarOutputStream;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
import org.ops4j.io.StreamUtils;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 11, 2008
 */
public class BndBundleBuilderTest
{

    @Test
    public void simpleCreate()
        throws IOException
    {
        ResourceLocator loc = createMock( ResourceLocator.class );
        loc.write( (JarOutputStream) ( notNull() ) );
        replay( loc );
        Properties p = new Properties();
        BndBundleBuilder b = new BndBundleBuilder( p, loc );
        InputStream in = b.build();
        assertNotNull( in );
        verify( loc );

        // we know its a jar
        //dumpToFile( in );

        FunctionalTest.dumpToConsole( in, 0 );
    }

    @Test( expected = IllegalArgumentException.class )
    public void failing()
        throws IOException
    {
        ResourceLocator loc = createMock( ResourceLocator.class );
        BndBundleBuilder b = new BndBundleBuilder( null, loc );
    }

    @Test( expected = IllegalArgumentException.class )
    public void failingWithoutLocator()
        throws IOException
    {
        BndBundleBuilder b = new BndBundleBuilder( new Properties(), null );
    }

   
   

    private void foo( InputStream in )
        throws IOException
    {
// verify bundle stream

        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream( pin );
        StreamUtils.copyStream( in, pout, false );

    }
}
