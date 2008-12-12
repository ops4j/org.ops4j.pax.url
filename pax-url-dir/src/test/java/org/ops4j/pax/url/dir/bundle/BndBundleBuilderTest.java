package org.ops4j.pax.url.dir.bundle;

import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
import org.ops4j.io.StreamUtils;
import org.ops4j.pax.url.dir.bundle.BundleBuilder;
import org.ops4j.pax.url.dir.ResourceLocator;
import org.ops4j.pax.url.dir.internal.FunctionalTest;

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
        BundleBuilder b = new BundleBuilder( p, loc );
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
        BundleBuilder b = new BundleBuilder( null, loc );
    }

    @Test( expected = IllegalArgumentException.class )
    public void failingWithoutLocator()
        throws IOException
    {
        BundleBuilder b = new BundleBuilder( new Properties(), null );
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
