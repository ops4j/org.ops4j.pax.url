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

import java.io.IOException;
import java.net.URL;
import org.junit.Test;

/**
 * Unit test for {@link Handler}.
 *
 * @author Toni Menzel
 * @since 1.2.0, Oct 01, 2010
 */
public class HandlerTest
{

    /**
     * Protocol handler can be used.
     *
     * @throws java.io.IOException - Unexpected
     */
    //@Test
    public void use()
        throws IOException
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        new URL( "aether:group/artifact/0.1.0" );
    }

   @Test
    public void cacheTest()
        throws IOException
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        
       System.setProperty( "http.proxyHost", "proxy.bb.poda.cz" );
           System.setProperty( "http.proxyPort", "8080" );

      //  System.setProperty( "org.ops4j.pax.url.aether.proxies","http:host=proxy.bb.poda.cz,port=3128" );

        new URL("aether:org.ops4j.pax.runner.profiles/log/LATEST/composite").openStream().close();

    }

     @Test
    public void sane()
        throws IOException
    {
 //       System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );

        System.setProperty( "http.proxyHost", "proxy.bb.poda.cz" );
               System.setProperty( "http.proxyPort", "8080" );

      //  System.setProperty( "org.ops4j.pax.url.aether.proxies","http:host=proxy.bb.poda.cz,port=3128" );

        new URL("http://google.com").openStream().close();

    }

}