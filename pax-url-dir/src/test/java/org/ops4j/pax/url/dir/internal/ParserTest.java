package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 10, 2008
 */
public class ParserTest
{

    @Test( expected = IllegalArgumentException.class )
    public void parseNotExisting()
        throws MalformedURLException
    {
        File f = new File( "doesnotexist" );
        String ext = f.toURI().toURL().toExternalForm();
        new Parser( "http:" + ext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void parseNotDirectory()
        throws MalformedURLException, URISyntaxException
    {
        File f = new File( this.getClass().getResource( "/test.txt" ).toURI() );

        String ext = f.toURI().toURL().toExternalForm();
        new Parser( "http:" + ext );
    }

    @Test
    public void parseValidURL()
        throws IOException
    {
        File f = new File( System.getProperty( "java.io.tmpdir" ) );

        // use dummy protocol for testing
        assertEquals( f.getCanonicalPath(),
                      new Parser( "http:" + f.getCanonicalPath() ).getDirectory().getAbsolutePath()
        );
    }

    @Test
    public void parseWithMarker()
        throws IOException, URISyntaxException
    {
        File f = new File( System.getProperty( "java.io.tmpdir" ) );
        Parser parser =
            new Parser( "http:" + f.getCanonicalPath() + "$tail=org/ops4j/pax/url/dir/internal/Activator.class" );
        // use dummy protocol for testing
        assertEquals( f.getCanonicalPath(), parser.getDirectory().getAbsolutePath() );
        assertEquals( "org/ops4j/pax/url/dir/internal/Activator.class", parser.getTailExpr() );
    }

    @Test
    public void parseWithMoreParams()
        throws IOException, URISyntaxException
    {
        Parser parser = new Parser( "http:." + "$a=1&b=2" );
        // use dummy protocol for testing

        assertEquals( "1", parser.getOptions().get( "a" ) );
        assertEquals( "2", parser.getOptions().get( "b" ) );
        assertEquals( 2, parser.getOptions().size() );
    }
}
