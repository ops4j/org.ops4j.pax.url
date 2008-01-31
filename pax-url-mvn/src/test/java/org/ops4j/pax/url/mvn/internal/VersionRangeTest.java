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
 * Unit test for VersionRange.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
public class VersionRangeTest
{

    /**
     * Tests that a null string for version range is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void nullVersionRange()
    {
        new VersionRange( null );
    }

    /**
     * Tests that an empty string for version range is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void emtyVersionRange()
    {
        new VersionRange( "" );
    }

    /**
     * Tests that a string with only spaces for version range is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void onlySpacesVersionRange()
    {
        new VersionRange( "  " );
    }

    /**
     * Tests that a version range must start with [ or (.
     */
    @Test( expected = IllegalArgumentException.class )
    public void incorrectStart()
    {
        new VersionRange( "1.0,1.1]" );
    }

    /**
     * Tests that a version range must end with ] or ).
     */
    @Test( expected = IllegalArgumentException.class )
    public void incorrectEnd()
    {
        new VersionRange( "[1.0,1.1" );
    }

    /**
     * Tests that a version range must contain "," (comma).
     */
    @Test( expected = IllegalArgumentException.class )
    public void noComma()
    {
        new VersionRange( "[1.0)" );
    }

    /**
     * Tests that a version range must contain only one "," (comma).
     */
    @Test( expected = IllegalArgumentException.class )
    public void onlyOneComma()
    {
        new VersionRange( "[1.0,2.0,3.0)" );
    }

    /**
     * Tests that a version range must have a start version that is not empty.
     */
    @Test( expected = IllegalArgumentException.class )
    public void notEmptyStartVersion()
    {
        new VersionRange( "[,3.0)" );
    }

    /**
     * Tests that a version range must have a end version that is not empty.
     */
    @Test( expected = IllegalArgumentException.class )
    public void notEmptyEndVersion()
    {
        new VersionRange( "[3.0,)" );
    }

    /**
     * Tests a valid range.
     */
    @Test
    public void validRange()
    {
        final VersionRange range = new VersionRange( "[1.0.0, 2.0.0)" );
        assertEquals( "From version", new Version( "1.0.0" ), range.getLowestVersion() );
        assertEquals( "To version", new Version( "2.0.0" ), range.getHighestVersion() );
    }

    /**
     * Tests that a version is in range.
     */
    @Test
    public void inRange()
    {
        final VersionRange range = new VersionRange( "[1.0.0, 2.0.0)" );
        assertTrue( "In range", range.includes( new Version( "1.5" ) ) );
    }

    /**
     * Tests that a version is in range when version eq lowest, inclusive.
     */
    @Test
    public void inRangeLowestInclusive()
    {
        final VersionRange range = new VersionRange( "[1.0.0, 2.0.0)" );
        assertTrue( "In range", range.includes( new Version( "1.0.0" ) ) );
    }

    /**
     * Tests that a version is not in range when version eq lowest, exclusive.
     */
    @Test
    public void inRangeLowestExclusive()
    {
        final VersionRange range = new VersionRange( "(1.0.0, 2.0.0)" );
        assertFalse( "Not in range", range.includes( new Version( "1.0.0" ) ) );
    }

    /**
     * Tests that a version is in range when version eq highest, inclusive.
     */
    @Test
    public void inRangeHighesttInclusive()
    {
        final VersionRange range = new VersionRange( "[1.0.0, 2.0.0]" );
        assertTrue( "In range", range.includes( new Version( "2.0.0" ) ) );
    }

    /**
     * Tests that a version is not in range when version eq highest, exclusive.
     */
    @Test
    public void inRangeHighestExclusive()
    {
        final VersionRange range = new VersionRange( "(1.0.0, 2.0.0)" );
        assertFalse( "Not in range", range.includes( new Version( "2.0.0" ) ) );
    }

}