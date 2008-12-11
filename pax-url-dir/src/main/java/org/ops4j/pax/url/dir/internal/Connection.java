package org.ops4j.pax.url.dir.internal;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;
import org.ops4j.pax.url.dir.Configuration;
import org.ops4j.lang.NullArgumentException;

/**
 * Accepts URLs like
 * dir:/Users/tonit/devel/pax/testing/$anchor=com.foo.Boo
 * dir:/Users/tonit/devel/pax/testing/$anchor=com.foo.Boo,Bundle-SymbolicName=HelloWorld

 * And even
 * * dir:mytest
 * which uses the relative dir mytest (from current one) without an acnhor.
 *
 * Why anchors ?
 * Sometimes, you don't know the real class folder
 *
 * @author Toni Menzel (tonit)
 * @since Dec 10, 2008
 */
public class Connection extends URLConnection
{

    private Configuration m_config;
    private Parser m_parser;

    public Connection( URL url, Configuration config )
    {
        super( url );
        NullArgumentException.validateNotNull( url, "url should be provided" );
        NullArgumentException.validateNotNull( config, "config should be provided" );

        m_config = config;
        try
        {
            m_parser = new Parser( url.toExternalForm() );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "URL " + url.getPath() + " is invalid", e );
        }

    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
        Properties p = new Properties();
        p.put( "Dynamic-Import", "*" );

        return new BndBundleBuilder( p, new IntelliResourceLocator( m_parser.getDirectory(), m_parser.getAnchor() )
        ).build();


    }

    public void connect()
        throws IOException
    {

    }
}
