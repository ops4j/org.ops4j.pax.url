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

package org.ops4j.pax.url.itest.mvn.osgi;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Tests the mvn: protocol handler.
 * 
 * @author Harald Wellmann
 */
@RunWith( PaxExam.class )
public class MvnProtocolTest
{

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config()
    {
        return options( frameworkProperty( "osgi.console" ).value( "6666" ), //
            systemProperty( "logback.configurationFile" ).value( "src/test/resources/logback.xml" ),
            systemProperty( "org.ops4j.pax.url.mvn.localRepository" ). //
            value( "target/local-repo-" + UUID.randomUUID() ), //

            bundle( "file:target/bundles/pax-url-aether.jar" ), //
            bundle( "file:target/bundles/slf4j-api.jar" ), //
            bundle( "file:target/bundles/jcl-over-slf4j.jar" ), //
            bundle( "file:target/bundles/logback-classic.jar" ), //
            bundle( "file:target/bundles/logback-core.jar" ), //
            bundle( "file:target/bundles/org.ops4j.pax.tipi.junit.jar" ), //
            bundle( "file:target/bundles/org.ops4j.pax.tipi.hamcrest.core.jar" ) );
    }

    @Test
    public void installFromMvnUrl() throws IOException, BundleException
    {
        assertThat( bc, is( notNullValue() ) );
        URL url = new URL( "mvn:org.ops4j.base/ops4j-base-lang/1.0.0" );

        // open stream of bundle resource
        InputStream is = url.openStream();
        assertThat( is, is( notNullValue() ) );

        // install bundle from stream
        Bundle bundle = bc.installBundle( "local", is );
        assertThat( bundle, is( notNullValue() ) );
        is.close();

        // bundle should be active
        assertThat( bundle.getState(), is( Bundle.ACTIVE ) );
    }
}
