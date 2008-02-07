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
package org.ops4j.pax.url.mvn.internal;

import java.net.MalformedURLException;
import java.net.URL;
import static org.junit.Assert.*;
import org.junit.Test;
import org.ops4j.lang.NullArgumentException;

public class RepositoryURLTest
{

    /**
     * Test that a null spec is not allowed.
     *
     * @throws java.net.MalformedURLException not expected
     */
    @Test( expected = NullArgumentException.class )
    public void nullSpec()
        throws MalformedURLException
    {
        new RepositoryURL( null );
    }

    /**
     * Test that empty spec is not allowed.
     *
     * @throws java.net.MalformedURLException not expected
     */
    @Test( expected = NullArgumentException.class )
    public void emptySpec()
        throws MalformedURLException
    {
        new RepositoryURL( "" );
    }

    /**
     * Test that spec that contains only spaces is not allowed.
     *
     * @throws java.net.MalformedURLException not expected
     */
    @Test( expected = NullArgumentException.class )
    public void onlySpacesSpec()
        throws MalformedURLException
    {
        new RepositoryURL( "  " );
    }

    /**
     * Test that by default snapshots are not allowed.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void defaultSnapshots()
        throws MalformedURLException
    {
        assertEquals( "Default snapshot enabled", false, new RepositoryURL( "file:some/dir" ).isSnapshotsEnabled() );
    }

    /**
     * Test that by default releases are allowed.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void defaultReleases()
        throws MalformedURLException
    {
        assertEquals( "Default releases enabled", true, new RepositoryURL( "file:some/dir" ).isReleasesEnabled() );
    }

    /**
     * Test url with snapshots enabled.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void enabledSnapshots()
        throws MalformedURLException
    {
        assertEquals( "Snapshots enabled", true, new RepositoryURL( "file:some/dir@snapshots" ).isSnapshotsEnabled() );
    }

    /**
     * Test url with releases disabled.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void disabledReleases()
        throws MalformedURLException
    {
        assertEquals( "Releases enabled", false, new RepositoryURL( "file:some/dir@noReleases" ).isReleasesEnabled() );
    }

    /**
     * Test url with releases disabled and snapshots enabled.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void disabledReleasesEnabledSnapshots()
        throws MalformedURLException
    {
        final RepositoryURL repositoryURL = new RepositoryURL( "file:some/dir@noreleases@snapshots" );
        assertEquals( "Releases enabled", false, repositoryURL.isReleasesEnabled() );
        assertEquals( "Snapshots enabled", true, repositoryURL.isSnapshotsEnabled() );
    }

    /**
     * Test url with @ character in url.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void atIncludedInUrl()
        throws MalformedURLException
    {
        assertEquals( "URL",
                      new URL( "http://user:password@somerepo/" ),
                      new RepositoryURL( "http://user:password@somerepo" ).toURL()
        );
    }

}