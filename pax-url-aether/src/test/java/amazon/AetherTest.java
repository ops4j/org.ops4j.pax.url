/*
 * Copyright (C) 2012 Andrei Pozolotin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package amazon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenConstants;
import org.ops4j.pax.url.maven.commons.MavenSettings;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;

/**
 */
public class AetherTest extends TestBase
{

    @Test
    public void resolveArtifact() throws DependencyCollectionException,
        ArtifactResolutionException, IOException
    {

        AetherBasedResolver resolver = new AetherBasedResolver( getTestConfig() );

        InputStream input = resolver.resolve( "org.ops4j.pax.web",
            "pax-web-api", "", "jar", "0.7.2" );

        input.close();

    }

    private MavenConfiguration getTestConfig() throws IOException
    {

        Properties props = new Properties();

        props.setProperty( //
            ServiceConstants.PID + MavenConstants.PROPERTY_LOCAL_REPOSITORY,//
            Util.getTestRepo().toURI().toASCIIString() //
            );

        props.setProperty( //
            ServiceConstants.PID + MavenConstants.PROPERTY_SETTINGS_FILE, //
            Util.getTestSettings().toURI().toASCIIString() //
            );

        MavenConfigurationImpl config = new MavenConfigurationImpl(
            new PropertiesPropertyResolver( props ), ServiceConstants.PID );

        File file = Util.getTestSettings();

        MavenSettings settings = new MavenSettingsImpl( file.toURI().toURL() );

        config.setSettings( settings );

        return config;

    }

}
