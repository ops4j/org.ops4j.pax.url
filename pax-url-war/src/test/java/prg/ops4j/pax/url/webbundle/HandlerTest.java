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
package prg.ops4j.pax.url.webbundle;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;
import org.ops4j.pax.url.war.internal.WarConnection;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for {@link org.ops4j.pax.url.webbundle.Handler}.
 *
 * @author Alin Dreghiciu
 * @since 1.0.0, June 16, 2009
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
            throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        URL url = new URL( "webbundle:file:foo.war?Import-Package=javax.servlet.jsp; version=\"[2.0,1000.0]\",javax.servlet.jsp.tagext; version=\"[2.0,1000.0]\"]&Web-ContextPath=/ct-testwar1_0" );
        WarConnection conn = (WarConnection) url.openConnection();
        Method mth = conn.getClass().getDeclaredMethod("getInstructions");
        mth.setAccessible(true);
        Properties props = (Properties) mth.invoke(conn);
        assertNotNull( props );
        assertNotNull( props.getProperty( "Import-Package" ) );
    }

}