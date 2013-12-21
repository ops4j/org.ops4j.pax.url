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

package org.ops4j.pax.url.mvn;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNotNull;

import java.io.File;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.junit.Test;

public class SettingsBuilderTest {

    @Test
    public void readMirrorWithEnvironmentVariable() throws SettingsBuildingException {
        assumeNotNull( System.getenv( "NEXUS_ROOT" ) );

        DefaultSettingsBuilderFactory factory = new DefaultSettingsBuilderFactory();
        DefaultSettingsBuilder builder = factory.newInstance();
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserSettingsFile( new File( "src/test/resources", "settings-mirror-env.xml" ) );
        
        SettingsBuildingResult result = builder.build( request );
        assertThat( result, is( notNullValue() ) );
        assertThat( result.getProblems().isEmpty(), is( true ) );
        
        Settings settings = result.getEffectiveSettings();
        assertThat( settings, is( notNullValue() ) );
        assertThat( settings.getMirrors().size(), is (1));
        
        Mirror mirror = settings.getMirrors().get(0);
        assertThat( mirror.getId(), is( "nexus" ) );
        assertThat( mirror.getMirrorOf(), is( "central" ) );
        assertThat( mirror.getUrl(), is( System.getenv( "NEXUS_ROOT" ) + "/content/groups/public" ) );
    }

}
