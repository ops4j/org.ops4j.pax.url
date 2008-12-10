package org.ops4j.pax.url.dir.internal;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.ops4j.pax.url.dir.Configuration;
import org.ops4j.lang.NullArgumentException;

/**
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
            m_parser = new Parser( url.getPath() );
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
        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);

        
        return pin;
    }

    public void connect()
        throws IOException
    {

    }
}
