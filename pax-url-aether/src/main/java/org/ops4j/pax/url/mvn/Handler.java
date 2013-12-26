/*
 * Copyright 2009 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenConstants;
import org.ops4j.pax.url.mvn.internal.Connection;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.ops4j.util.property.PropertyResolver;

/**
 * {@link URLStreamHandler} implementation for "mvn:" protocol.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @author Toni Menzel (adreghiciu@gmail.com)

 * @since 1.3.0, March 28, 2011 (usable since)
 */
public class Handler
    extends URLStreamHandler
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected URLConnection openConnection( final URL url )
        throws IOException
    {
        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver( System.getProperties() );
        final MavenConfigurationImpl config = new MavenConfigurationImpl( propertyResolver, ServiceConstants.PID);
        
        config.setSettings( buildSettings( getLocalRepoPath( propertyResolver ), getSettingsPath( config ), config.useFallbackRepositories() ) );
        return new Connection( url, config );
    }
    
    private String getSettingsPath( MavenConfigurationImpl config ) {
        URL url = config.getSettingsFileUrl();
        return url == null ? null : url.getPath();
    }
    
    private String getLocalRepoPath(PropertyResolver props) {
        return props.get( ServiceConstants.PID + MavenConstants.PROPERTY_LOCAL_REPOSITORY );
    }
    
    private Settings buildSettings( String localRepoPath, String settingsPath, boolean useFallbackRepositories ) {
        Settings settings;
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
                settings = result.getEffectiveSettings();
            }
            catch( SettingsBuildingException exc ) {
                throw new AssertionError( "cannot build settings", exc );
            }

        }
        if( useFallbackRepositories ) {
            Profile fallbackProfile = new Profile();
            Repository central = new Repository();
            central.setId( "central" );
            central.setUrl( "http://repo1.maven.org/maven2" );
            fallbackProfile.setId( "fallback" );
            fallbackProfile.setRepositories( Arrays.asList( central ) );
            settings.addProfile( fallbackProfile );
            settings.addActiveProfile( "fallback" );
        }
        if (localRepoPath != null) {
            settings.setLocalRepository( localRepoPath );
        }
        return settings;
    }

}