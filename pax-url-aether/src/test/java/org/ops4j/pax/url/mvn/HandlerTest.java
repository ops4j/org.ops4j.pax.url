/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.url.mvn;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link Handler}.
 *
 * @author Toni Menzel
 * @since 1.2.0, Oct 01, 2010
 */
public class HandlerTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    /**
     * Protocol handler can be used.
     *
     * @throws java.io.IOException - Unexpected
     */
    @Test
    public void use()
        throws IOException
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        new URL( "mvn:group/artifact/0.1.0" );
    }

    @Test
    public void cacheTest()
           throws IOException, URISyntaxException {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );

        //avoid depending on local settings property
        System.setProperty("org.ops4j.pax.url.mvn.settings", getClass().getResource("/settings-no-mirror.xml").toURI().getPath());
        System.setProperty("org.ops4j.pax.url.mvn.useFallbackRepositories", "true");

        new URL("mvn:org.ops4j.base/ops4j-base-lang/1.1.0").openStream().close();

    }

    @Test
    public void noFallbackRepositoryTest()
           throws IOException, URISyntaxException {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );

        //avoid depending on local settings property
        System.setProperty("org.ops4j.pax.url.mvn.settings", getClass().getResource("/settings-no-mirror.xml").toURI().getPath());
        System.getProperties().remove("org.ops4j.pax.url.mvn.useFallbackRepositories");

        thrown.expect( IOException.class );
        thrown.expectMessage( "Error resolving artifact");
        new URL("mvn:org.ops4j.pax.runner.profiles/log/LATEST/composite").openStream().close();
    }
}