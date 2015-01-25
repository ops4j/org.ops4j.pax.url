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

package org.ops4j.pax.url.itest.mvn;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalRepositoryTest
{

    @Before
    public void before()
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
    }

    @After
    public void after()
    {
        System.clearProperty( "java.protocol.handler.pkgs" );
        System.clearProperty( "org.ops4j.pax.url.mvn.localRepository" );
        System.clearProperty( "org.ops4j.pax.url.mvn.settings" );
    }

    @Test
    public void resolveArtifactWithCustomLocalRepo() throws IOException
    {
        String localRepoPath = "target/local-repo-" + UUID.randomUUID().toString();
        System.setProperty( "org.ops4j.pax.url.mvn.localRepository", localRepoPath );
        File localRepo = new File( localRepoPath );
        localRepo.mkdirs();

        URL url = new URL( "mvn:org.ops4j.base/ops4j-base-lang/1.0.0" );
        url.openStream().close();

        File artifact = new File( localRepo,
            "org/ops4j/base/ops4j-base-lang/1.0.0/ops4j-base-lang-1.0.0.jar" );
        assertThat( artifact.exists(), is( true ) );
    }

    @Test
    public void resolveArtifactWithDefaultLocalRepo() throws IOException
    {
        File localRepo = new File( System.getProperty( "user.home" ), ".m2/repository" );
        File artifact = new File( localRepo,
            "org/ops4j/base/ops4j-base-lang/1.0.0/ops4j-base-lang-1.0.0.jar" );

        if( artifact.exists() )
        {
            artifact.delete();
            assertThat( artifact.exists(), is( false ) );
        }

        URL url = new URL( "mvn:org.ops4j.base/ops4j-base-lang/1.0.0" );
        url.openStream().close();

        assertThat( artifact.exists(), is( true ) );
    }

    @Test
    public void resolveArtifactWithLocalRepoFromSettings() throws IOException
    {
        String localRepoPath = "target/local-repo-settings";
        System.setProperty( "org.ops4j.pax.url.mvn.settings", "src/test/resources/settings-local-repo.xml" );
        File localRepo = new File( localRepoPath );
        localRepo.mkdirs();

        URL url = new URL( "mvn:org.ops4j.base/ops4j-base-lang/1.0.0" );
        url.openStream().close();

        File artifact = new File( localRepo,
            "org/ops4j/base/ops4j-base-lang/1.0.0/ops4j-base-lang-1.0.0.jar" );
        assertThat( artifact.exists(), is( true ) );
    }

}
