package org.ops4j.pax.url.dir.internal;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 10, 2008
 */
public class ParserTest
{

    @Test
    public void parseValidURL()
        throws MalformedURLException
    {
        File f = new File (System.getProperty("java.io.tmpdir") );
        // Parser needs it as url
        String ext = f.toURL().toExternalForm();

        // use dummy protocol for testing
        assertEquals( f.getPath(), new Parser( "http:" + ext ).getDirectory().getAbsolutePath() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void parseNotExisting()
        throws MalformedURLException
    {
        File f = new File( "doesnotexist" );
        String ext = f.toURL().toExternalForm();
        new Parser( "http:" + ext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void parseNotDirectory()
        throws MalformedURLException, URISyntaxException
    {
        File f = new File( this.getClass().getResource( "/test.txt" ).toURI() );

        String ext = f.toURL().toExternalForm();
        new Parser( "http:" + ext );
    }
}
