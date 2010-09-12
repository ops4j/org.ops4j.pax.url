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
package org.ops4j.pax.url.aether;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.InputStreamFacade;
import org.junit.Test;
import org.ops4j.io.FileUtils;

/**
 * Unit test for {@link Handler}.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, March 12, 2008
 */
public class HandlerTest {

    /**
     * Protocol handler can be used.
     *
     * @throws java.io.IOException - Unexpected
     */
    @Test
    public void use()
            throws IOException {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        new URL("aether:group/artifact/0.1.0");
    }

    @Test
    public void resolveTest()
            throws IOException {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

        final URL url = new URL("aether:org.ops4j.pax.exam/pax-exam/1.2.1");
        
        org.codehaus.plexus.util.FileUtils.copyStreamToFile(new InputStreamFacade() {

            public InputStream getInputStream() throws IOException {
                return url.openStream();
            }
        }, new File("target/resolved.jar"));
    }

}