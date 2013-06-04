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
package org.ops4j.pax.url.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link Handler}.
 * 
 * @author Harald Wellmann (harald.wellmann@gmx.de)
 * @since 1.3.5, Aug 5, 2011
 */
public class HandlerTest
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp()
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
    }

    @Test
    public void openAndDereference() throws IOException
    {
        URL url = new URL( "reference:file:/tmp/pax/someDir/" );
        assertNotNull( url );
        InputStream is = url.openStream();
        assertTrue( is instanceof ReferenceInputStream );

        ReferenceInputStream refStream = (ReferenceInputStream) is;
        URL reference = refStream.getReference();
        assertEquals( "file:/tmp/pax/someDir/", reference.toString() );
    }

    @Test
    public void exceptionOnWrongSubprotocol() throws IOException
    {
        expectedException.expect( MalformedURLException.class );
        expectedException.expectMessage( "'file:'" );

        URL url = new URL( "reference:http://www.ops4j.org" );
        url.openStream();
    }

    @Test
    public void exceptionOnNoSubprotocol() throws IOException
    {
        expectedException.expect( MalformedURLException.class );
        expectedException.expectMessage( "'file:'" );

        URL url = new URL( "reference:/road/to/nowhere" );
        url.openStream();
    }

    @Test
    public void exceptionIfEmpty() throws IOException
    {
        expectedException.expect( MalformedURLException.class );
        expectedException.expectMessage( "empty" );

        URL url = new URL( "reference:" );
        url.openStream();
    }

    @Test
    public void exceptionIfBlank() throws IOException
    {
        expectedException.expect( MalformedURLException.class );
        expectedException.expectMessage( "empty" );

        URL url = new URL( "  reference:   " );
        url.openStream();
    }
}
