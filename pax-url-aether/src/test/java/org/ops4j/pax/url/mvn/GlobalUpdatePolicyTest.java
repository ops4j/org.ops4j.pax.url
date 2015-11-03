/*
 * Copyright (C) 2013 Andrei Pozolotin
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
package org.ops4j.pax.url.mvn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify snapshot updates, with help of global repository update policy.
 */
public class GlobalUpdatePolicyTest
{

    /**
     * Test project used to deploy snapshots.
     */
    static final String GROUP = "org.ops4j.pax.url";
    static final String ARTIFACT = "pax-url-aether-test";
    static final String VERSION = "1.0.0-SNAPSHOT";
    static final String TYPE = "jar";

    /**
     * Well known file system locations.
     */
    static final File WORK = new File( System.getProperty( "user.dir" ) );
    static final File HOME = new File( System.getProperty( "user.home" ) );

    /**
     * Location of test project resources.
     */
    static final File PROJECT = new File( WORK, ARTIFACT );
    static final File POM = new File( PROJECT, "pom.xml" );

    /**
     * Provide URL of test project remote repository.
     */
    static final File SETTINGS = new File( "target/test-classes",
        "settings-ops4j-snapshots-1.xml" );

    /**
     * Alternative local maven repository used by test. Preserved between builds and system
     * restarts.
     */
    static final File REPO = new File( HOME, "org.ops4j.pax.url/test-repo" );
    static final File REMOTE_REPO = new File( "target/remote-repo" );
    static
    {
        UnitHelp.ensureFolder( REPO );
        UnitHelp.ensureFolder( REMOTE_REPO );
    }

    /**
     * Maven home system property.
     * <p>
     * During maven build, provided by maven-surefire-plugin.
     * <p>
     * During IDE guild, discovered from O/S PATH.
     */
    static final String PROP_MAVEN_HOME = "maven.home";

    protected final Logger LOG = LoggerFactory
        .getLogger( GlobalUpdatePolicyTest.class );

    /**
     * Deploy snapshot of test project to the remote repository.
     * <p>
     * Use alternative local repository different from default user repository.
     */
    private void mavenDeploy() throws Exception
    {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setLocalRepositoryDirectory( REPO );
        request.setPomFile( POM );
        request.setGoals( Collections.singletonList( "deploy" ) );
        Properties properties = new Properties();
        properties.setProperty( "TEST_REPO", REMOTE_REPO.toURI().toURL().toString() );
        request.setProperties( properties );

        Invoker invoker = new DefaultInvoker();

        /** IDE invocation requires maven home discovery. */
        if( System.getProperty( PROP_MAVEN_HOME ) == null )
        {
            File mavenHome = UnitHelp.getMavenHome();
            System.setProperty( PROP_MAVEN_HOME, mavenHome.getAbsolutePath() );
            LOG.info( "{} = {}", PROP_MAVEN_HOME, mavenHome );
        }

        InvocationResult result = invoker.execute( request );

        assertTrue( "deploy success", result.getExitCode() == 0 );

    }

    /**
     * Use custom test settings.
     */
    private MavenConfiguration testConfig() throws Exception
    {

        final Properties props = new Properties();

        /** Relax SSL requirements. */
        props.setProperty( ServiceConstants.PID + "."
                + ServiceConstants.PROPERTY_CERTIFICATE_CHECK, "false" );

        /** Enable snapshot update on every resolve request. */
        props.setProperty( ServiceConstants.PID + "."
                + ServiceConstants.PROPERTY_GLOBAL_UPDATE_POLICY, "always" );

        MavenConfiguration config = UnitHelp.getConfig( SETTINGS, props );

        assertEquals( false, config.getCertificateCheck() );
        assertEquals( "always", config.getGlobalUpdatePolicy() );

        return config;

    }

    /**
     * Deploy two snapshots in sequence, resolve and ensure proper time stamp relations.
     */
    @Test
    public void verifySnapshotUpdates() throws Exception
    {

        final AetherBasedResolver resolver = new AetherBasedResolver( testConfig() );

        LOG.info( "init" );

        final long time0 = System.currentTimeMillis();

        LOG.info( "first" );
        final long time1;
        {
            mavenDeploy();

            final File file = //
                resolver.resolve( GROUP, ARTIFACT, "", TYPE, VERSION );

            time1 = file.lastModified();
        }

        LOG.info( "second" );
        final long time2;
        {
            mavenDeploy();

            final File file = //
                resolver.resolve( GROUP, ARTIFACT, "", TYPE, VERSION );

            time2 = file.lastModified();
        }

        LOG.info( "verify" );

        assertTrue( time0 > 0 );
        assertTrue( time1 > 0 );
        assertTrue( time2 > 0 );

        assertTrue( "first is fresh", time1 > time0 );
        assertTrue( "second is fresh", time2 > time0 );

        assertTrue( "second after frirst", time2 > time1 );

        LOG.info( "done" );

        resolver.close();
    }

}
