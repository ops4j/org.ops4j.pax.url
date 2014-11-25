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
package org.ops4j.pax.url.mvn.s3mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Test;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.UnitHelp;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;

public class AetherTest extends TestBase
{

    @Test
    public void resolveArtifact() throws DependencyCollectionException,
        ArtifactResolutionException, IOException
    {

        AetherBasedResolver resolver = new AetherBasedResolver( getTestConfig() );

        resolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "0.7.2" );

    }

    private MavenConfiguration getTestConfig() throws IOException
    {

        Properties props = new Properties();

        props.setProperty( //
            ServiceConstants.PID + "." + ServiceConstants.PROPERTY_LOCAL_REPOSITORY,//
            Util.getTestRepo().toURI().toASCIIString() //
            );

        props.setProperty( //
            ServiceConstants.PID + "." + ServiceConstants.PROPERTY_SETTINGS_FILE, //
            Util.getTestSettings().toURI().toASCIIString() //
            );

        File file = Util.getTestSettings();

        return UnitHelp.getConfig( file, props );

    }

}
