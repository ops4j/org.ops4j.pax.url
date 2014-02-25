/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal.config;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.junit.Test;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;
import org.ops4j.util.property.PropertyResolver;

public class ConfigurationImplTest
{

    private static final String PID = "org.ops4j.pax.url.mvn";

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullResolver()
    {
        new MavenConfigurationImpl( null, PID );
    }

    @Test
    public void getCertificateCheck()
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.certificateCheck" ) ).andReturn( "true" );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Certificate check", true, config.getCertificateCheck() );
        verify( propertyResolver );
    }

    @Test
    public void getDefaultCertificateCheck()
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.certificateCheck" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Certificate check", false, config.getCertificateCheck() );
        verify( propertyResolver );
    }

    @Test
    public void getSettingsAsURL()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn(
            "file:somewhere/settings.xml"
        );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Settings", new URL( "file:somewhere/settings.xml" ), config.getSettingsFileUrl() );
        verify( propertyResolver );
    }

    @Test
    public void getSettingsAsFilePath()
        throws MalformedURLException, FileNotFoundException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        File validSettings = FileUtils.getFileFromClasspath( "configuration/settings.xml" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn(
            validSettings.getAbsolutePath()
        );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Settings", validSettings.toURI().toURL(), config.getSettingsFileUrl() );
        verify( propertyResolver );
    }

    /**
     * Test that a malformed url will not trigger an exception ( will be skipped)
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void getSettingsAsMalformedURL()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( "noprotocol:settings.xml" );
        replay( propertyResolver );
        new MavenConfigurationImpl( propertyResolver, PID ).getSettingsFileUrl();
    }

    @Test
    public void getSettingsWithoutPropertySet()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        URL settings = config.getSettingsFileUrl();
        assertNotNull( settings );
        assertEquals( "Settings", "file:" + System.getProperty("user.home") + "/.m2/settings.xml", settings.toExternalForm() );
        verify( propertyResolver );
    }

    @Test
    public void getGetLocalRepository()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( "file:/user/home/.m2/repository" ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        MavenRepositoryURL localRepo = config.getLocalRepository();
        assertNotNull( localRepo );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithOneRepository()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( "file:repository1/@id=repository1" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 1, repositories.size() );
        assertEquals( "Repository", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithoutSlash()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( "file:repository1@id=repository1" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 1, repositories.size() );
        assertEquals( "Repository", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithSlash()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( "file:repository1/@id=repository1" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 1, repositories.size() );
        assertEquals( "Repository", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithBackSlash()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( "file:repository1\\@id=repository1" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 1, repositories.size() );
        assertEquals( "Repository", new URL( "file:repository1\\" ), repositories.get( 0 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithAtSnapshotsAndAtNoReleases()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn(
            "http:repository1@snapshots@noreleases@id=repository1"
        );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertTrue( "Repository as snapshot enabled:", repositories.get( 0 ).isSnapshotsEnabled() );
        assertFalse( "Repository as release enabled:", repositories.get( 0 ).isReleasesEnabled() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithAtSnapshots()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn(
            "http:repository1@snapshots@id=repository1"
        );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertTrue( "Repository as snapshot enabled:", repositories.get( 0 ).isSnapshotsEnabled() );
        assertTrue( "Repository as release enabled:", repositories.get( 0 ).isReleasesEnabled() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithAtNoReleases()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn(
            "http:repository1@noreleases@id=repository1"
        );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertFalse( "Repository as snapshot not enabled:", repositories.get( 0 ).isSnapshotsEnabled() );
        assertFalse( "Repository as release not enabled:", repositories.get( 0 ).isReleasesEnabled() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithMoreRepositories()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn(
            "file:repository1/@id=repository1,file:repository2/@id=repository2"
        );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 2, repositories.size() );
        assertEquals( "Repository 1", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        assertEquals( "Repository 2", new URL( "file:repository2/" ), repositories.get( 1 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesFromSettings()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        Settings settings = settingsForRepository( "repository1", "file:repository1" );
        replay( propertyResolver );
        MavenConfigurationImpl config = new MavenConfigurationImpl( propertyResolver, PID );
        config.setSettings( settings );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 1, repositories.size() );
        assertEquals( "Repository", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        verify( propertyResolver );
    }
    
    private Settings settingsForRepository(String id, String url) 
    {
        Settings settings = new Settings();
        Profile profile = new Profile();
        profile.setId( "test" );
        Repository repo = new Repository();
        repo.setId( id );
        repo.setUrl( url );
        profile.addRepository( repo );
        settings.addProfile( profile );
        settings.addActiveProfile( "test" );
        return settings;        
    }

    @Test
    public void getRepositoriesFromConfigAndSettings()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( "+file:repository1/@id=repository1" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        Settings settings = settingsForRepository( "repository2", "file:repository2" );
        replay( propertyResolver );
        MavenConfigurationImpl config = new MavenConfigurationImpl( propertyResolver, PID );
        config.setSettings( settings );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 2, repositories.size() );
        assertEquals( "Repository 1", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        assertEquals( "Repository 2", new URL( "file:repository2/" ), repositories.get( 1 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getRepositoriesWithLocalRepository()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.repositories" ) ).andReturn( "file:repository1/@id=repository1" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            "file:localRepository/"
        ).atLeastOnce();
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.defaultLocalRepoAsRemote" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        List<MavenRepositoryURL> repositories = config.getRepositories();
        assertNotNull( "Repositories is null", repositories );
        assertEquals( "Repositories size", 1, repositories.size() );
        assertEquals( "Local repository", new URL( "file:localRepository/" ), config.getLocalRepository().getURL()
        );
        assertEquals( "Repository", new URL( "file:repository1/" ), repositories.get( 0 ).getURL() );
        verify( propertyResolver );
    }

    @Test
    public void getLocalRepositoryAsURL()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            "file:somewhere/localrepository/"
        ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Local repository",
                      new URL( "file:somewhere/localrepository/" ),
                      config.getLocalRepository().getURL()
        );
        verify( propertyResolver );
    }

    @Test
    public void getLocalRepositoryAsURLWithoutSlash()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            "file:somewhere/localrepository"
        ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Local repository",
                      new URL( "file:somewhere/localrepository/" ),
                      config.getLocalRepository().getURL()
        );
        verify( propertyResolver );
    }

    @Test
    public void getLocalRepositoryAsURLWithBackSlash()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            "file:somewhere/localrepository\\"
        ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Local repository",
                      new URL( "file:somewhere/localrepository\\" ),
                      config.getLocalRepository().getURL()
        );
        verify( propertyResolver );
    }

    @Test
    public void getLocalRepositoryAsFilePath()
        throws MalformedURLException, FileNotFoundException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        File valid = FileUtils.getFileFromClasspath( "configuration/localrepository" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            valid.getAbsolutePath()
        ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Local repository",
                      valid.toURI().toURL(),
                      config.getLocalRepository().getURL()
        );
        verify( propertyResolver );
    }

    @Test
    public void getLocalRepositoryAsFilePathWithoutSlash()
        throws MalformedURLException, FileNotFoundException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        File valid = FileUtils.getFileFromClasspath( "configuration/localrepository" );
        File validWithSlash = FileUtils.getFileFromClasspath( "configuration/localrepository/" );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            valid.getAbsolutePath()
        ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Local repository",
                      validWithSlash.toURI().toURL(),
                      config.getLocalRepository().getURL()
        );
        verify( propertyResolver );
    }

    /**
     * Test that an url that is malformed will not trigger an exception.
     *
     * @throws MalformedURLException never
     */
    @Test
    public void getLocalRepositoryAsMalformedURL()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            "noprotocol://localrepository"
        ).atLeastOnce();
        replay( propertyResolver );
        new MavenConfigurationImpl( propertyResolver, PID ).getLocalRepository();
    }

    /**
     * Test that a path to a file that does not exist will not trigger an exception.
     *
     * @throws MalformedURLException never
     */
    @Test
    public void getLocalRepositoryToAnInexistentDirectory()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn(
            "c:/this/should/be/an/inexistent/directory"
        ).atLeastOnce();
        replay( propertyResolver );
        new MavenConfigurationImpl( propertyResolver, PID ).getLocalRepository();
    }

    @Test
    public void getLocalRepositoryWithoutPropertySet()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null ).atLeastOnce();
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        MavenRepositoryURL localRepo = config.getLocalRepository();
        assertNotNull( localRepo );
        assertEquals( "Local repository", System.getProperty("user.home") + "/.m2/repository", localRepo.getFile().toString() );
        verify( propertyResolver );
    }

    @Test
    public void getLocalRepositoryFromSettings()
        throws MalformedURLException
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null ).atLeastOnce();
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        Settings settings = createMock( Settings.class );
        expect( settings.getLocalRepository() ).andReturn( "file:somewhere/localrepository/" );
        replay( propertyResolver, settings );
        MavenConfigurationImpl config = new MavenConfigurationImpl( propertyResolver, PID );
        config.setSettings( settings );
        assertEquals( "Local repository",
                      new URL( "file:somewhere/localrepository/" ),
                      config.getLocalRepository().getURL()
        );
        verify( propertyResolver, settings );
    }

    @Test
    public void useFallbackRepositories()
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( "true" );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals( "Use Fallback Repositories", true, config.useFallbackRepositories() );
        verify( propertyResolver );
    }

    @Test
    public void defaultUseFallbackRepositories()
    {
        PropertyResolver propertyResolver = createMock( PropertyResolver.class );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.localRepository" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.settings" ) ).andReturn( null );
        expect( propertyResolver.get( "org.ops4j.pax.url.mvn.useFallbackRepositories" ) ).andReturn( null );
        replay( propertyResolver );
        MavenConfiguration config = new MavenConfigurationImpl( propertyResolver, PID );
        assertEquals("Use Fallback Repositories", true, config.useFallbackRepositories());
        verify( propertyResolver );
    }

}
