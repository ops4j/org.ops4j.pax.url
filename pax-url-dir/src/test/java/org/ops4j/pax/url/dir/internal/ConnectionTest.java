package org.ops4j.pax.url.dir.internal;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.ops4j.pax.url.dir.Configuration;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 11, 2008
 */
public class ConnectionTest
{

    @Test
    public void simple()
        throws IOException 
    {
        String clazz = this.getClass().getName().replaceAll( "\\.", "/" ) + ".class";
        URL url = new URL( "http:.$anchor=" + clazz + ",Bubba=Foo");
        Configuration config = createMock( Configuration.class );

        Connection con = new Connection( url, config );
        InputStream inp = con.getInputStream();
        FunctionalTest.dumpToConsole( inp ,14);
    }

    // @Test
    public void simpleWithoutAnchor()
        throws IOException
    {
        URL url = new URL( "http:pax-url-dir/target/test-classes" );
        Configuration config = createMock( Configuration.class );

        Connection con = new Connection( url, config );
        InputStream inp = con.getInputStream();
        FunctionalTest.dumpToConsole( inp ,14);
    }
}
