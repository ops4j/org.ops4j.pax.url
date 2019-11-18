package org.ops4j.pax.url.mvn.internal;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_FAIL;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_INTERVAL;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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

        settings.setLocalRepository( "target/localrepo_" + UUID.randomUUID() );
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
            "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
    }

    @Test( expected = IOException.class )
    public void no_mirror_notfound() throws IOException {
        MavenConfigurationImpl config = getConfig( "settings-mirror2.xml",
            "fake", "http://qfdqfqfqf.fra/repo" );
        Settings settings = config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL(  null, "mvn:test/test/1.5.1", new org.ops4j.pax.url.mvn.Handler() ),
                                       new AetherBasedResolver( config ) );
        c.getInputStream().read();
    }

    @Test
    public void mirror2() throws IOException, InterruptedException {
        MavenConfigurationImpl config = getConfig( "settings-mirror2.xml",
            "fake", "http://qfdqfqfqf.fra/repo" );

        Settings settings = config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        // you must exist.
        localRepo.mkdirs();

        Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler() ),
                                       new AetherBasedResolver( config ) );
        c.getInputStream();
        assertEquals( "the artifact must be downloaded", true, new File( localRepo,
            "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
    }

	@Test
	public void mirror3() throws IOException, InterruptedException {
		MavenConfigurationImpl config = getConfig("settings-mirror3.xml",
				"fake", "http://qfdqfqfqf.fra/repo");

		Settings settings = config.getSettings();
		File localRepo = new File(settings.getLocalRepository());
		// you must exist.
		localRepo.mkdirs();

		Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler() ),
                                       new AetherBasedResolver( config ) );
		c.getInputStream();
		assertEquals("the artifact must be downloaded", true, new File(
				localRepo, "ant/ant/1.5.1/ant-1.5.1.jar").exists());
	}

	@Test
	public void mirrorBlankPath() throws Exception {

		File file = new File("target/test-classes/settings-mirror3.xml");
		File blankPath = new File("target/test-classes/path with blanks/");
		File blankPathFile = new File(blankPath.getAbsolutePath(),
				"settings.xml");
		blankPath.mkdirs();

		if (!blankPathFile.exists())
			Files.copy(file.toPath(), blankPathFile.toPath());

		MavenConfigurationImpl config = getConfig(
				"path with blanks/settings.xml", "fake",
				"http://qfdqfqfqf.fra/repo");

		Settings settings = config.getSettings();
		File localRepo = new File(settings.getLocalRepository());
		// you must exist.
		localRepo.mkdirs();

		Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler() ),
                                       new AetherBasedResolver( config ) );
		c.getInputStream();
		assertEquals("the artifact must be downloaded", true, new File(
				localRepo, "ant/ant/1.5.1/ant-1.5.1.jar").exists());

	}
	
	@Test
	public void mirror1MultiRemotes() throws IOException, InterruptedException {

		Repository disabledRemote = new Repository(), enabledRemote = new Repository();
		enabledRemote.setId( "enabled" );
		enabledRemote.setUrl( "http://google.com/repo" );
		disabledRemote.setId( "disabled" );
		disabledRemote.setUrl( "http://google.com/repo" );
		RepositoryPolicy disabledPolicy = new RepositoryPolicy();
		disabledPolicy.setEnabled( false );
		disabledRemote.setReleases( disabledPolicy );

		MavenConfigurationImpl config = getConfig( "settings-mirror1.xml", enabledRemote, disabledRemote);

		Settings settings = config.getSettings();
		File localRepo = new File( settings.getLocalRepository() );
		// you must exist.
		localRepo.mkdirs();

		Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler() ),
									   new AetherBasedResolver( config ) );
		c.getInputStream();
		assertEquals( "the artifact must be downloaded", true, new File( localRepo,
			"ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
	}

	@Test
	public void mirror1MultiRemote_configCorrect() {

		Repository remote1 = new Repository(), remote2 = new Repository();
		remote1.setId( "remote1" );
		remote1.setUrl( "http://google.com/repo" );
		remote2.setId( "remote2" );
		remote2.setUrl( "http://google.com/repo" );
		RepositoryPolicy remote1Policy = new RepositoryPolicy();
		remote1Policy.setEnabled( true );
		remote1Policy.setUpdatePolicy( UPDATE_POLICY_INTERVAL + ":42");
		remote1Policy.setChecksumPolicy( CHECKSUM_POLICY_FAIL );
		remote1.setReleases( remote1Policy );
		RepositoryPolicy remote2Policy = new RepositoryPolicy();
		remote2Policy.setEnabled( true );
		remote2Policy.setUpdatePolicy( UPDATE_POLICY_NEVER );
		remote2Policy.setChecksumPolicy( CHECKSUM_POLICY_WARN );
		remote2.setReleases( remote2Policy );

		@SuppressWarnings("resource")
		List<RemoteRepository> mirrors1 = new AetherBasedResolver( getConfig( "settings-mirror1.xml", remote1, remote2 ) ).getRepositories();		

		assertNotNull( mirrors1 ) ;
		assertEquals( 1, mirrors1.size() );
		assertEquals( true, mirrors1.get( 0 ).getPolicy( false ).isEnabled() );
		assertEquals( "interval:42", mirrors1.get( 0 ).getPolicy( false ).getUpdatePolicy() );
		assertEquals( CHECKSUM_POLICY_FAIL, mirrors1.get( 0 ).getPolicy( false ).getChecksumPolicy() );

		@SuppressWarnings("resource")
		List<RemoteRepository> mirrors2 = new AetherBasedResolver( getConfig( "settings-mirror1.xml", remote2, remote1 ) ).getRepositories();		

		assertNotNull( mirrors2) ;
		assertEquals( 1, mirrors2.size() );
		assertEquals( true, mirrors2.get( 0 ).getPolicy( false ).isEnabled() );
		assertEquals( "interval:42", mirrors2.get( 0 ).getPolicy( false ).getUpdatePolicy() );
		assertEquals( CHECKSUM_POLICY_FAIL, mirrors2.get( 0 ).getPolicy( false ).getChecksumPolicy() );

	}

	@Test
	public void mirrorFromSysProperty() throws IOException, InterruptedException {

		MavenConfigurationImpl config = getConfig("settings-no-mirror.xml", "fake", "http://qfdqfqfqf.fra/repo");
		File localRepo = new File(config.getSettings().getLocalRepository());
		localRepo.mkdirs();

		String previous = System.getProperty(ServiceConstants.PROPERTY_MAVEN_MIRROR_URL_SYS);
		try {
			System.setProperty(ServiceConstants.PROPERTY_MAVEN_MIRROR_URL_SYS, "my-mirror::http://localhost:" + System.getProperty("jetty.http.port"));
			Properties p = new Properties();
			MavenConfigurationImpl config2 = new MavenConfigurationImpl(new PropertiesPropertyResolver(
					p), ServiceConstants.PID);
			config.getSettings().setMirrors(config2.getSettings().getMirrors());

			Connection c = new Connection(new URL(null, "mvn:ant/ant/1.5.1", new org.ops4j.pax.url.mvn.Handler()),
					new AetherBasedResolver(config));
			c.getInputStream();
			assertEquals("the artifact must be downloaded", true, new File(localRepo,
					"ant/ant/1.5.1/ant-1.5.1.jar").exists());

			assertEquals("my-mirror", config.getSettings().getMirrors().get(0).getId());
		} finally {
			if (previous != null) {
				System.setProperty(ServiceConstants.PROPERTY_MAVEN_MIRROR_URL_SYS, previous);
			}
		}
	}

}
