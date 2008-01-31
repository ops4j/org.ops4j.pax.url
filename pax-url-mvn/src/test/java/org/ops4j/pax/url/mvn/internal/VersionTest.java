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
package org.ops4j.pax.url.mvn.internal;

import static org.junit.Assert.*;
import org.junit.Test;
import org.ops4j.lang.NullArgumentException;

/**
 * Unit test for Version.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
public class VersionTest
{

    /**
     * Tests that a null string for version is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void constructorWithNull()
    {
        new Version( null );
    }

    /**
     * Tests that an empty string for version is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void constructorWithEmptyString()
    {
        new Version( "" );
    }

    /**
     * Tests that a string with only spaces for version is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void constructorWithOnlySpacesString()
    {
        new Version( "  " );
    }

    /**
     * Test a version with one string segment.
     */
    @Test
    public void oneStringSegment()
    {
        final Version version = new Version( "RC1" );
        final VersionSegment[] segments = version.getSegments();
        assertArrayEquals( "Segments",
                           new Object[]{ new StringVersionSegment( "RC1" ) },
                           segments
        );
    }

    /**
     * Test a version with two string segment.
     */
    @Test
    public void twoStringSegments()
    {
        final Version version = new Version( "RC1.SNAPSHOT" );
        final VersionSegment[] segments = version.getSegments();
        assertArrayEquals( "Segments",
                           new Object[]{
                               new StringVersionSegment( "RC1" ),
                               new StringVersionSegment( "SNAPSHOT" )
                           },
                           segments
        );
    }

    /**
     * Test a version with one integer segment.
     */
    @Test
    public void oneIntegerSegment()
    {
        final Version version = new Version( "1" );
        final VersionSegment[] segments = version.getSegments();
        assertArrayEquals( "Segments",
                           new Object[]{ new IntegerVersionSegment( 1 ) },
                           segments
        );
    }

    /**
     * Test a version with two integer segments.
     */
    @Test
    public void twoIntegerSegments()
    {
        final Version version = new Version( "1.2" );
        final VersionSegment[] segments = version.getSegments();
        assertArrayEquals( "Segments",
                           new Object[]{
                               new IntegerVersionSegment( 1 ),
                               new IntegerVersionSegment( 2 )
                           },
                           segments
        );
    }

    /**
     * Test a version with combined segments.
     */
    @Test
    public void combinedSegments()
    {
        final Version version = new Version( "1.2.0-SNAPSHOT" );
        final VersionSegment[] segments = version.getSegments();
        assertArrayEquals( "Segments",
                           new Object[]{
                               new IntegerVersionSegment( 1 ),
                               new IntegerVersionSegment( 2 ),
                               new IntegerVersionSegment( 0 ),
                               new StringVersionSegment( "SNAPSHOT" )
                           },
                           segments
        );
    }

    /**
     * Test that a version as "." is allowed.
     */
    @Test
    public void validVersion01()
    {
        new Version( "." );
    }

    /**
     * Test that a version as ".." is allowed.
     */
    @Test
    public void validVersion02()
    {
        new Version( ".." );
    }

    /**
     * Test that a version as ".RC1." is allowed.
     */
    @Test
    public void validVersion03()
    {
        new Version( ".RC1." );
    }

    /**
     * Tests that 1.0 < 1.1
     */
    @Test
    public void compare01()
    {
        assertTrue( "1.0 < 1.1", new Version( "1.0" ).compareTo( new Version( "1.1" ) ) == -1 );
    }

    /**
     * Tests that 1.0-SNAPSHOT < 1.0
     */
    @Test
    public void compare02()
    {
        assertTrue( "1.0.SNAPSHOT < 1.0", new Version( "1.0.SNAPSHOT" ).compareTo( new Version( "1.0" ) ) == -1 );
    }

    /**
     * Tests that 1.0 < 2
     */
    @Test
    public void compare03()
    {
        assertTrue( "1.0 < 2", new Version( "1.0" ).compareTo( new Version( "2" ) ) == -1 );
    }

    /**
     * Tests that 1.0 == 1.0.
     */
    @Test
    public void compare04()
    {
        assertTrue( "1.0 == 1.0.", new Version( "1.0" ).compareTo( new Version( "1.0." ) ) == 0 );
    }

    /**
     * Tests that 1.5 < 2.0.0
     */
    @Test
    public void compare05()
    {
        assertTrue( "1.5 < 2.0.0", new Version( "1.5" ).compareTo( new Version( "2.0.0" ) ) == -1 );
    }

    /**
     * Tests equals.
     */
    @Test
    public void equals()
    {
        assertEquals( "Versions", new Version( "1.0" ), new Version( "1.0" ) );
    }

}
