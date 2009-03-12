package org.ops4j.pax.url.dir.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import static org.easymock.EasyMock.*;
import org.junit.Test;

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
        URL url = new URL( "http:.$tail=" + clazz + "&Foo=bar" );
        Configuration config = createMock( Configuration.class );

        Connection con = new Connection( url, config );
        InputStream inp = con.getInputStream();
        FunctionalTest.dumpToConsole( inp, 17 );
    }

}
