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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RepositoryPolicyTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File localRepo;

    @Before
    public void before()
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        String localRepoPath = "target/local-repo-" + UUID.randomUUID().toString();
        System.setProperty( "org.ops4j.pax.url.mvn.localRepository", localRepoPath );
        localRepo = new File( localRepoPath );
        localRepo.mkdirs();
    }

    @After
    public void after()
    {
        System.clearProperty( "java.protocol.handler.pkgs" );
        System.clearProperty( "org.ops4j.pax.url.mvn.localRepository" );
        System.clearProperty( "org.ops4j.pax.url.mvn.repositories" );
    }

    @Test
    public void resolveRelease() throws IOException
    {
        System.setProperty( "org.ops4j.pax.url.mvn.repositories",
            "http://www.knopflerfish.org/maven2/@id=kf" );
        URL url = new URL( "mvn:org.knopflerfish/framework/7.0.1/pom" );
        url.openStream().close();

        File artifact = new File( localRepo,
            "org/knopflerfish/framework/7.0.1/framework-7.0.1.pom" );
        assertThat( artifact.exists(), is( true ) );
    }

    @Test
    //@Ignore("PAXURL-235")
    public void doNotResolveReleaseWhenReleasesDisabled() throws IOException
    {
        System.setProperty( "org.ops4j.pax.url.mvn.repositories",
            "http://www.knopflerfish.org/maven2/@id=kf@noreleases" );
        URL url = new URL( "mvn:org.knopflerfish/framework/7.0.1/pom" );

        thrown.expect( IOException.class );
        url.openStream().close();
    }

    @Test
    public void resolveSnapshotWhenSnapshotsEnabled() throws IOException
    {
        System.setProperty( "org.ops4j.pax.url.mvn.repositories",
            "https://oss.sonatype.org/content/repositories/ops4j-snapshots@id=ops4j-snapshots@snapshots" );
        URL url = new URL( "mvn:org.ops4j/master/2.0.1-SNAPSHOT/pom" );
        url.openStream().close();

        File artifact = new File( localRepo,
            "org/ops4j/master/2.0.1-SNAPSHOT/master-2.0.1-SNAPSHOT.pom" );
        assertThat( artifact.exists(), is( true ) );
    }

    @Test
    //@Ignore("PAXURL-235")
    public void doNotResolveSnapshotWhenSnapshotsDisabled() throws IOException
    {
        System.setProperty( "org.ops4j.pax.url.mvn.repositories",
            "https://oss.sonatype.org/content/repositories/ops4j-snapshots@id=ops4j-snapshots" );
        URL url = new URL( "mvn:org.ops4j/master/2.0.1-SNAPSHOT/pom" );
        
        thrown.expect( IOException.class );
        url.openStream().close();
    }
}
