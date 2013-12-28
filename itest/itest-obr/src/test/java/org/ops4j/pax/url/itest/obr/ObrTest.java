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

package org.ops4j.pax.url.itest.obr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
 * Tests the obr: protocol handler.
 *
 * @author Harald Wellmann
 */
@RunWith(PaxExam.class)
public class ObrTest
{
    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config()
    {
        return options(
            systemProperty( "logback.configurationFile" ).value( "src/test/resources/logback.xml" ),
            frameworkProperty("osgi.console").value("6666"),

            // OBR location
            frameworkProperty("obr.repository.url").value("http://felix.apache.org/obr/releases.xml"),
            // bundle under test and its dependencies
            mavenBundle("org.ops4j.pax.url", "pax-url-obr").versionAsInProject(),
            mavenBundle("org.ops4j.pax.url", "pax-url-commons").versionAsInProject(),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-property").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.osgi.service.obr", "1.0.2"),

            // OBR RepositoryAdmin implementation
            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository", "1.6.6"),
            
            mavenBundle( "org.slf4j", "slf4j-api").versionAsInProject(),
            mavenBundle( "ch.qos.logback", "logback-classic" ).versionAsInProject(),
            mavenBundle( "ch.qos.logback", "logback-core" ).versionAsInProject(),

            junitBundles() );
    }



    @Test
    public void installFromObrUrl() throws IOException, BundleException {
        // check that obr: handler is available
        URL url = new URL("obr:org.apache.felix.ipojo/1.8.0");
        assertThat( url, is( notNullValue()) );

        // open stream of OBR resource
        InputStream is = url.openStream();
        assertThat( is, is( notNullValue()) );

        // install bundle from stream
        Bundle bundle = bc.installBundle( "local", is );
        assertThat( bundle, is(notNullValue()));

        // bundle should be active
        assertThat( bundle.getState(), is(Bundle.ACTIVE));
    }
}
