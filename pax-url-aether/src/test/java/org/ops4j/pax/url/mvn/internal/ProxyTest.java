package org.ops4j.pax.url.mvn.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.url.mvn.UnitHelp;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenConstants;

public class ProxyTest
{
    private static final String TEST_PID = "org.ops4j.pax.url.mvn";
    private Server server;

    @Before
    public void startHttp() throws Exception
    {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort( 8778 );
        server.addConnector( connector );

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed( false );
        resource_handler.setWelcomeFiles( new String[]{} );

        resource_handler.setResourceBase( "target/test-classes/repo2" );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[]{ resource_handler, new DefaultHandler() } );
        server.setHandler( handlers );

        server.start();
    }

    @After
    public void stopHttp() throws Exception
    {
        server.stop();
    }

    @Test
    public void proxy1() throws Exception
    {
        String repoPath = "target/localrepo_" + UUID.randomUUID();

        Properties properties = new Properties();
        properties.setProperty( TEST_PID + MavenConstants.PROPERTY_LOCAL_REPOSITORY, repoPath );
        properties.setProperty( TEST_PID + MavenConstants.PROPERTY_REPOSITORIES,
            "http://qfdqfqfqf.fra@id=fake" );

        File file = new File( "target/test-classes/settings-proxy1.xml" );
        MavenConfiguration config = UnitHelp.getConfig( file, properties );
        File localRepo = new File( repoPath );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( "file:ant/ant/1.5.1" ), config );
        c.getInputStream();

        assertEquals( "the artifact must be downloaded", true, new File( localRepo,
            "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
        
        // test for PAXURL-209
        assertThat( System.getProperty( "http.proxyHost" ), is( nullValue() ) );
    }
}
