/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.url.wrap.internal;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import static org.junit.Assert.*;
import org.junit.Test;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.swissbox.bnd.OverwriteMode;

public class ParserTest
{

    @Test( expected = MalformedURLException.class )
    public void nullUrl()
        throws MalformedURLException
    {
        new Parser( null, true );
    }

    @Test( expected = MalformedURLException.class )
    public void emptyUrl()
        throws MalformedURLException
    {
        new Parser( " ", true );
    }

    @Test( expected = MalformedURLException.class )
    public void urlStartingWithInstructionsSeparator()
        throws MalformedURLException
    {
        new Parser( "$instructions", true );
    }

    @Test( expected = MalformedURLException.class )
    public void urlEndingWithInstructionsSeparator()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$", true );
    }

    @Test
    public void validWrappedJarURL()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        assertNotNull( "Properties was not expected to be null", parser.getWrappedJarURL() );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructionsURL()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar,wrongprotocol:toInstructions", true );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructions01()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$instructions", true );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructions02()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$Bundle-SymbolicName&Bundle-Name", true );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructions03()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$Bundle-SymbolicName&", true );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructions04()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$&Bundle-Name", true );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructions05()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$Bundle-SymbolicName&Bundle-Name=v2", true );
    }

    @Test( expected = MalformedURLException.class )
    public void validWrappedJarURLAndInvalidInstructions06()
        throws MalformedURLException
    {
        new Parser( "file:toWrap.jar$Bundle-SymbolicName=v1&Bundle-Name", true );
    }

    @Test
    public void validWrappedJarURLAndValidInstructions()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar$Bundle-SymbolicName=v1&Bundle-Name=v2", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        Properties props = parser.getWrappingProperties();
        assertNotNull( "Properties was not expected to be null", props );
        assertEquals( "Property 1", "v1", props.getProperty( "Bundle-SymbolicName" ) );
        assertEquals( "Property 2", "v2", props.getProperty( "Bundle-Name" ) );
    }

    @Test
    public void validWrappedJarURLAndValidOneInstruction()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar$Bundle-SymbolicName=v1", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        Properties props = parser.getWrappingProperties();
        assertNotNull( "Properties was not expected to be null", props );
        assertEquals( "Property 1", "v1", props.getProperty( "Bundle-SymbolicName" ) );
    }

    @Test
    public void validWrappedJarURLAndValidInstructionsURL()
        throws MalformedURLException, FileNotFoundException
    {
        Parser parser = new Parser(
            "file:toWrap.jar,"
            + FileUtils.getFileFromClasspath( "parser/instructions.properties" ).toURI().toURL().toExternalForm(), true
        );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        Properties props = parser.getWrappingProperties();
        assertNotNull( "Properties was not expected to be null", props );
        assertEquals( "Property 1", "v1", props.getProperty( "Bundle-SymbolicName" ) );
        assertEquals( "Property 2", "v2", props.getProperty( "Bundle-Name" ) );
    }

    @Test
    public void validWrappedJarURLAndValidInstructionsURLFromJar()
        throws MalformedURLException, FileNotFoundException
    {
        Parser parser = new Parser(
            "file:toWrap.jar,jar:"
            + FileUtils.getFileFromClasspath( "parser/instructions.jar" ).toURI().toURL().toExternalForm()
            + "!/instructions.properties", true
        );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        Properties props = parser.getWrappingProperties();
        assertNotNull( "Properties was not expected to be null", props );
        assertEquals( "Property 1", "v1", props.getProperty( "Bundle-SymbolicName" ) );
        assertEquals( "Property 2", "v2", props.getProperty( "Bundle-Name" ) );
    }

    @Test
    public void validWrappedJarURLAndValidInstructionsURLAndInstructions()
        throws MalformedURLException, FileNotFoundException
    {
        Parser parser = new Parser(
            "file:toWrap.jar,"
            + FileUtils.getFileFromClasspath( "parser/instructions.properties" ).toURI().toURL().toExternalForm()
            + "$Bundle-Name=v3&Bundle-URL=v4", true
        );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        Properties props = parser.getWrappingProperties();
        assertNotNull( "Properties was not expected to be null", props );
        assertEquals( "Property 1", "v1", props.getProperty( "Bundle-SymbolicName" ) );
        assertEquals( "Property 2", "v3", props.getProperty( "Bundle-Name" ) );
        assertEquals( "Property 3", "v4", props.getProperty( "Bundle-URL" ) );
    }

    @Test
    public void defaultOverwriteMode()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        assertEquals( "Overwrite mode", OverwriteMode.KEEP, parser.getOverwriteMode() );
    }

    @Test
    public void invalidOverwriteMode()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar$overwrite=invalid", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        assertEquals( "Overwrite mode", OverwriteMode.KEEP, parser.getOverwriteMode() );
    }

    @Test
    public void mergeOverwriteMode()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar$overwrite=merge", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        assertEquals( "Overwrite mode", OverwriteMode.MERGE, parser.getOverwriteMode() );
    }

    @Test
    public void fullOverwriteMode()
        throws MalformedURLException
    {
        Parser parser = new Parser( "file:toWrap.jar$overwrite=full", true );
        assertEquals( "Wrapped Jar URL", new URL( "file:toWrap.jar" ), parser.getWrappedJarURL() );
        assertEquals( "Overwrite mode", OverwriteMode.FULL, parser.getOverwriteMode() );
    }

}
