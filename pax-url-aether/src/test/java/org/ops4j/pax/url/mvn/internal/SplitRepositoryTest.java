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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.repository.RemoteRepository;
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
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_FAIL;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_INTERVAL;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SplitRepositoryTest {

    private Server server;

    @Before
    public void startHttp() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        String portNumber = System.getProperty( "jetty.http.port" );
        connector.setPort( Integer.parseInt( portNumber ) );
        server.addConnector( connector );

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed( false );
        resource_handler.setWelcomeFiles( new String[] {} );

        resource_handler.setResourceBase( "target/test-classes/repo2" );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[] { resource_handler, new DefaultHandler() } );
        server.setHandler( handlers );

        server.start();
    }

    @After
    public void stopHttp() throws Exception {
        server.stop();
    }

    private Settings buildSettings( String settingsPath, String id, String url ) {
        Repository remote = new Repository();
        remote.setId( id );
        remote.setUrl( url );

        return buildSettings(settingsPath, remote);
    }

    private Settings buildSettings( String settingsPath, Repository... remotes ) {
        Settings settings = null;
        if( settingsPath == null ) {
            settings = new Settings();
        }
        else {
            DefaultSettingsBuilderFactory factory = new DefaultSettingsBuilderFactory();
            DefaultSettingsBuilder builder = factory.newInstance();
            SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setSystemProperties(System.getProperties());
            request.setUserSettingsFile( new File( "target/test-classes", settingsPath ) );
            try {
                SettingsBuildingResult result = builder.build( request );
                assertThat( result, is( notNullValue() ) );
                assertThat( result.getProblems().isEmpty(), is( true ) );

                settings = result.getEffectiveSettings();
            }
            catch( SettingsBuildingException exc ) {
                throw new AssertionError( "cannot build settings", exc );
            }
        }

        Profile centralProfile = new Profile();
        centralProfile.setId( "test" );
        for ( Repository remote : remotes )
        	centralProfile.addRepository( remote );
        settings.addProfile( centralProfile );
        settings.addActiveProfile( "test" );
        return settings;
    }

    private MavenConfigurationImpl getConfig( String settingsPath, String id, String url ) {
        Properties p = new Properties();
        p.setProperty(ServiceConstants.PID + ".localRepository", "target/localrepo_" + UUID.randomUUID()
                + "@id=local@split@splitRemote@splitRemotePrefix=remote@splitRemoteRepository");
        MavenConfigurationImpl config = new MavenConfigurationImpl( new PropertiesPropertyResolver(
            p ), ServiceConstants.PID );
        config.setSettings( buildSettings( settingsPath, id, url ) );
        return config;
    }

    private MavenConfigurationImpl getConfig( String settingsPath, Repository... remotes ) {
        Properties p = new Properties();
        MavenConfigurationImpl config = new MavenConfigurationImpl( new PropertiesPropertyResolver(
            p ), ServiceConstants.PID );
        config.setSettings( buildSettings( settingsPath, remotes ) );
        return config;
    }

    @Test
    public void mirror1() throws IOException, InterruptedException {

        MavenConfigurationImpl config = getConfig( "settings-mirror1.xml",
            "fake", "http://google.com/repo" );

        Settings settings = config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler() ),
                                       new AetherBasedResolver( config ) );
        c.getInputStream();
        assertEquals( "the artifact must be downloaded", true, new File( localRepo,
            "remote/mirror1/releases/ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
    }

}
