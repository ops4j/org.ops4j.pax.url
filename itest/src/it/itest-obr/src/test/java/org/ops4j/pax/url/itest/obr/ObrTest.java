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
import static org.ops4j.pax.exam.CoreOptions.*;

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
            bundle( "file:target/bundles/pax-url-obr.jar" ),
            bundle( "file:target/bundles/pax-url-commons.jar" ),
            bundle( "file:target/bundles/pax-swissbox-property.jar" ),
            bundle( "file:target/bundles/org.osgi.service.obr.jar" ),
            // OBR RepositoryAdmin implementation
            bundle( "file:target/bundles/org.apache.felix.bundlerepository.jar" ),
            
            bundle( "file:target/bundles/slf4j-api.jar" ), //
            bundle( "file:target/bundles/logback-classic.jar" ), //
            bundle( "file:target/bundles/logback-core.jar" ), //
            bundle( "file:target/bundles/org.ops4j.pax.tipi.junit.jar" ), //
            bundle( "file:target/bundles/org.ops4j.pax.tipi.hamcrest.core.jar" ) );
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
