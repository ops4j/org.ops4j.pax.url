package org.ops4j.pax.url.dir.internal.bundle;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarOutputStream;

import org.junit.Test;
import org.ops4j.pax.url.dir.internal.FunctionalTest;
import org.ops4j.pax.url.dir.internal.ResourceLocator;

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
        new BundleBuilder( null, loc );
    }

    @Test( expected = IllegalArgumentException.class )
    public void failingWithoutLocator()
        throws IOException
    {
        new BundleBuilder( new Properties(), null );
    }
}
