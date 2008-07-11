package org.ops4j.pax.url.mvnlive;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.ops4j.pax.runner.handler.internal.URLStreamHandlerExtender;
import org.ops4j.pax.url.mvnlive.internal.Configuration;
import org.ops4j.pax.url.mvnlive.internal.ConfigurationImpl;
import org.ops4j.pax.url.mvnlive.internal.Connection;
import org.ops4j.util.property.PropertyResolver;

/**
 * @author Toni Menzel (tonit)
 * @since Jul 10, 2008
 */
public class ConnectionTest
{

    private URLStreamHandlerExtender m_extender;

    //@Test
    public void connectionFormatFullLocalPath()
        throws IOException
    {

        // this will trigger anther project to build.
        Connection conn =
            new Connection( new URL( "mvnlive:file:///Users/tonit/devel/pax/url/pax-url-bnd" ), new Configuration()
            {

            }
            );
        assertTrue( conn.getInputStream().available() > 0 );

    }

    @Test
    public void conectionFormatArtifact()
        throws IOException
    {

        // this will trigger anther project to build.
        Connection conn =
            new Connection( new URL( "mvnlive:org.ops4j.pax.url/pax-url-bnd" ), new Configuration()
            {

            }
            );
        assertTrue( conn.getInputStream().available() > 0 );

    }

    @Before
    public void setup()
    {
        // add m2 home:
        System.setProperty( "M2_HOME","/Users/tonit/devel/maven" );

        // add handler, which can be scipped if using paxdrone with paxrunner connector later.
        m_extender = new URLStreamHandlerExtender();
        m_extender.register( new String[]{ "mvnlive" }, new AbstractURLStreamHandlerService()
        {

            public URLConnection openConnection( URL url )
                throws IOException
            {
                return new Connection( url, new ConfigurationImpl( new PropertyResolver()
                {

                    public String get( String s )
                    {
                        return null;
                    }
                }
                )
                );
            }
        }
        );
        m_extender.start();
    }

    @After
    public void end()
    {
        m_extender.unregister( new String[]{ "mvnlive" } );
    }
}
