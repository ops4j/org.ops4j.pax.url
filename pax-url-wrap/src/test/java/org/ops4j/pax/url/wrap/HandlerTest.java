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
package org.ops4j.pax.url.wrap;

import java.io.IOException;
import java.net.URL;
import org.junit.Test;

/**
 * Unit test for {@link Handler}.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, March 12, 2008
 */
public class HandlerTest
{

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
        new URL( "wrap:file:foo.jar" );
    }

}