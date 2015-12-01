/*
 * Copyright 2013 Harald Wellmann
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.ops4j.pax.url.mvn.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ops4j.pax.url.mvn.Handler;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

public class MirrorAuthenticationTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Server server;

    @SuppressWarnings( "restriction" )
    @Before
    public void startHttp() throws Exception
    {
        /*
         * Oracle JDK specific workaround to disable HTTP authentication caching.
         * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6626700.
         * 
         * The cache would break our tests for failing authentication.
         */
        sun.net.www.protocol.http.AuthCacheValue.setAuthCache( new sun.net.www.protocol.http.AuthCacheImpl() );
        
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort( 8778 );
        server.addConnector( connector );

        LoginService loginService = new HashLoginService( "Maven Repository",
            "src/test/resources/realm.properties" );
        server.addBean( loginService );

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler( security );

        Constraint constraint = new Constraint();
        constraint.setName( "auth" );
        constraint.setAuthenticate( true );
        constraint.setRoles( new String[]
            { "user", "admin" } );

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec( "/*" );
        mapping.setConstraint( constraint );

        security.setConstraintMappings( Collections.singletonList( mapping ) );
        security.setAuthenticator( new BasicAuthenticator() );
        security.setLoginService( loginService );

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed( false );
        resourceHandler.setWelcomeFiles( new String[]
            {} );

        resourceHandler.setResourceBase( "target/test-classes/repo2" );
        security.setHandler( resourceHandler );

        server.start();
    }

    @After
    public void stopHttp() throws Exception
    {
        System.clearProperty( PaxUrlSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION );
        server.stop();
    }

    private Settings buildSettings( String settingsPath, String id, String url )
    {
        Settings settings = null;
        if( settingsPath == null )
        {
            settings = new Settings();
        }
        else
        {
            DefaultSettingsBuilderFactory factory = new DefaultSettingsBuilderFactory();
            DefaultSettingsBuilder builder = factory.newInstance();
            SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile( new File( settingsPath ) );
            try
            {
                SettingsBuildingResult result = builder.build( request );
                assertThat( result, is( notNullValue() ) );
                assertThat( result.getProblems().isEmpty(), is( true ) );

                settings = result.getEffectiveSettings();
            }
            catch( SettingsBuildingException exc )
            {
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

    private MavenConfigurationImpl getConfig( String settingsPath, String id, String url )
    {
        Properties p = new Properties();
        MavenConfigurationImpl config = new MavenConfigurationImpl( new PropertiesPropertyResolver(
            p ), ServiceConstants.PID );
        config.setSettings( buildSettings( settingsPath, id, url ) );
        return config;
    }

    @Test
    public void authenticationShouldFail() throws IOException, InterruptedException
    {
        MavenConfigurationImpl config = getConfig( "src/test/resources/settings-mirror-auth-fail.xml",
        		 "fake", "http://google.com/repo" );

        Settings settings = config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        localRepo.mkdirs();

        Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new Handler() ),
                                       new AetherBasedResolver( config ) );

        thrown.expect( IOException.class );
        thrown.expectMessage( "Not authorized" );
        c.getInputStream();
    }

    @Test
    public void authenticationShouldPassWithAMirrorWithAName() throws IOException, InterruptedException
    {
        MavenConfigurationImpl config = getConfig( "src/test/resources/settings-mirror-with-name-auth-pass.xml",
        		 "fake", "http://google.com/repo" );

        Settings settings = config.getSettings();
        File localRepo = new File( settings.getLocalRepository() );
        localRepo.mkdirs();

        Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new Handler() ),
                                       new AetherBasedResolver( config ) );
        c.getInputStream().close();
        assertEquals( "the artifact must be downloaded", true, new File( localRepo,
            "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
    }
    
    @Test
	public void authenticationShouldPassWithAMirrorWithoutName() throws Exception {
    	MavenConfigurationImpl config = getConfig( "src/test/resources/settings-mirror-without-name-auth-pass.xml",
       		 "fake", "http://google.com/repo" );

       Settings settings = config.getSettings();
       File localRepo = new File( settings.getLocalRepository() );
       localRepo.mkdirs();

       Connection c = new Connection( new URL( null, "mvn:ant/ant/1.5.1", new Handler() ),
                                      new AetherBasedResolver( config ) );
       c.getInputStream().close();
       assertEquals( "the artifact must be downloaded", true, new File( localRepo,
           "ant/ant/1.5.1/ant-1.5.1.jar" ).exists() );
	}
}
