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
package org.ops4j.pax.url.mvn.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenConstants;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for mvn: protocol handler
 */
public final class Activator
    extends HandlerActivator<MavenConfiguration>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, org.ops4j.pax.url.commons.handler.ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<MavenConfiguration>()
            {

                /**
                 * @see ConnectionFactory#createConection(BundleContext, URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final MavenConfiguration config )
                    throws MalformedURLException
                {
                    return new Connection( url, config );
                }

                /**
                 * @see ConnectionFactory#createConfiguration(org.ops4j.util.property.PropertyResolver)
                 */
                public MavenConfiguration createConfiguration( final PropertyResolver propertyResolver )
                {
                    final MavenConfigurationImpl config =
                        new MavenConfigurationImpl( propertyResolver, ServiceConstants.PID );
                    if (!config.isValid())
                    {
                        return null;
                    }
                    config.setSettings( buildSettings( getLocalRepoPath( propertyResolver ), getSettingsPath( config ), config.useFallbackRepositories() ) );
                    return config;
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
        );
    }

}