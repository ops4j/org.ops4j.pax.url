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

import org.junit.Test;
import org.ops4j.lang.NullArgumentException;

/**
 * Unit test for StringVersionSegment.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
public class IntegerVersionSegmentTest
{

    /**
     * Tests that a null string for version is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void constructorWithNullOnString()
    {
        new IntegerVersionSegment( (String) null );
    }

    /**
     * Tests that a null integer for version is not allowed.
     */
    @Test( expected = NullArgumentException.class )
    public void constructorWithNullOnInteger()
    {
        new IntegerVersionSegment( (Integer) null );
    }

    /**
     * Tests that an empty string for version is not allowed.
     */
    @Test( expected = NumberFormatException.class )
    public void emptyStringSegment()
    {
        new IntegerVersionSegment( "" );
    }

    /**
     * Tests that an non empty string that cannot be converted to a number is not allowed.
     */
    @Test( expected = NumberFormatException.class )
    public void invalidNonEmptyStringSegment()
    {
        new IntegerVersionSegment( "RC1" );
    }

    /**
     * Tests that an non empty string that can be converted to an integer is allowed.
     */
    @Test
    public void validStringSegment()
    {
        new IntegerVersionSegment( "1" );
    }

    /**
     * Tests that an non null integer is allowed.
     */
    @Test
    public void validIntegerSegment()
    {
        new IntegerVersionSegment( 1 );
    }

}