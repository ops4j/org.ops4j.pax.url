/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.UnitHelp;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class ProxyMirrorTest
{
    private static final String TEST_PID = "org.ops4j.pax.url.mvn";
    private Server server;

    @Before
    public void startHttp() throws Exception
    {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
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
        properties.setProperty( TEST_PID + "." + ServiceConstants.PROPERTY_LOCAL_REPOSITORY, repoPath );
        properties.setProperty( TEST_PID + "." + ServiceConstants.PROPERTY_REPOSITORIES,
            "http://qfdqfqfqf.fra@id=fake" );

        File file = new File( "target/test-classes/settings-mirror-proxy.xml" );
        MavenConfiguration config = UnitHelp.getConfig( file, properties );
        File localRepo = new File( repoPath );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler()),
                                       new AetherBasedResolver( config ) );
        c.getInputStream();

        assertTrue("the artifact must be downloaded", new File(localRepo,
                "ant/ant/1.5.1/ant-1.5.1.jar").exists());
        
        // test for PAXURL-209
        assertThat( System.getProperty( "http.proxyHost" ), is( nullValue() ) );
    }
}
