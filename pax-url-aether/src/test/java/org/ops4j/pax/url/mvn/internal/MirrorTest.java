package org.ops4j.pax.url.mvn.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

public class MirrorTest {

    private Server server;

    @Before
    public void startHttp() throws Exception {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort( 8181 );
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
        Settings settings = null;
        if( settingsPath == null ) {
            settings = new Settings();
        }
        else {
            DefaultSettingsBuilderFactory factory = new DefaultSettingsBuilderFactory();
            DefaultSettingsBuilder builder = factory.newInstance();
            SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile( new File( settingsPath ) );
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

        settings.setLocalRepository( "target/localrepo_" + UUID.randomUUID() );
        Profile centralProfile = new Profile();
        centralProfile.setId( "test" );
        Repository remote = new Repository();
        remote.setId( id );
        remote.setUrl( url );
        centralProfile.addRepository( remote );
        settings.addProfile( centralProfile );
        settings.addActiveProfile( "test" );
        return settings;
    }

    private MavenConfigurationImpl getConfig( String settingsPath, String id, String url ) {
        Properties p = new Properties();
        MavenConfigurationImpl config = new MavenConfigurationImpl( new PropertiesPropertyResolver(
            p ), ServiceConstants.PID );
        config.setSettings( buildSettings( settingsPath, id, url ) );
        return config;
    }

    @Test
    public void mirror1() throws IOException, InterruptedException {

        MavenConfigurationImpl config = getConfig( "src/test/resources/settings-mirror1.xml",
            "fake", "http://google.com/repo" );

        Settings settings = (Settings) config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( "file:ant/ant/1.5.1" ), config );
        c.getInputStream();
        Assert.assertEquals( "the artifact must be downloaded", true, new File( localRepo,
            "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
    }

    @Test( expected = IOException.class )
    public void no_mirror_notfound() throws IOException {
        MavenConfigurationImpl config = getConfig( "src/test/resources/settings-mirror2.xml",
            "fake", "http://qfdqfqfqf.fra/repo" );
        Settings settings = (Settings) config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( "file:test/test/1.5.1" ), config );
        c.getInputStream().read();
    }

    @Test
    public void mirror2() throws IOException, InterruptedException {
        MavenConfigurationImpl config = getConfig( "src/test/resources/settings-mirror2.xml",
            "fake", "http://qfdqfqfqf.fra/repo" );

        Settings settings = (Settings) config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( "file:ant/ant/1.5.1" ), config );
        c.getInputStream();
        Assert.assertEquals( "the artifact must be downloaded", true, new File( localRepo,
            "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
    }
}
