/*
 * Copyright 2012 Andrei Pozolotin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.s3mock;

import java.io.InputStream;
import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

/**
 */
public class HandlerTest extends TestBase
{

    @Test
    @Ignore
    public void test() throws Exception
    {
        String settings = Util.getTestSettings().getAbsolutePath();
        String localRepo = Util.getTestRepo().getAbsolutePath();

        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );

        System.setProperty( "org.ops4j.pax.url.mvn.settings", settings );

        System.setProperty( "org.ops4j.pax.url.mvn.localRepository", localRepo );

        System.setProperty( "org.ops4j.pax.url.mvn.globalChecksumPolicy", "ignore" );

        System.setProperty( "org.ops4j.pax.url.mvn.certificateCheck", "false" );

        System.setProperty( "org.ops4j.pax.url.mvn.useFallbackRepositories",
            "false" );

        InputStream input = new URL( "mvn:org.ops4j.base/ops4j-base-lang/1.1.0" )
            .openStream();

        // Thread.sleep(100 * 1000);

        input.close();

    }

}
