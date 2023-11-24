/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal.config;

import java.net.MalformedURLException;
import java.net.URI;

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
        new MavenRepositoryURL( null );
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
        new MavenRepositoryURL( "" );
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
        new MavenRepositoryURL( "  " );
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
        assertFalse("Default snapshot enabled", new MavenRepositoryURL("file:some/dir@id=repository1").isSnapshotsEnabled());
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
        assertTrue("Default releases enabled", new MavenRepositoryURL("file:some/dir@id=repository1").isReleasesEnabled());
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
        assertTrue("Snapshots enabled", new MavenRepositoryURL("file:some/dir@snapshots@id=repository1").isSnapshotsEnabled());
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
        assertFalse("Releases enabled", new MavenRepositoryURL("file:some/dir@noReleases@id=repository1").isReleasesEnabled());
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
        final MavenRepositoryURL repositoryURL = new MavenRepositoryURL( "file:some/dir@noreleases@snapshots@id=repository1" );
        assertFalse("Releases enabled", repositoryURL.isReleasesEnabled());
        assertTrue("Snapshots enabled", repositoryURL.isSnapshotsEnabled());
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
        // remember NOT to call URL.equals, as this leads to DNS resolution inside
        // java.net.URLStreamHandler.hostsEqual()!
        assertEquals( "URL",
                      URI.create( "http://user:password@somerepo/" ).toString(),
                      new MavenRepositoryURL( "http://user:password@somerepo@id=repository1" ).getURI().toString()
        );
    }

    @Test
    public void testRepositoryWithoutIDFallback()
        throws MalformedURLException
    {
        String spec = "file:some/dir" ;
        assertTrue( "Computed ID", new MavenRepositoryURL( spec ).getId().startsWith( "repo_" ) );
    }

    /**
     * Test the repository id given in the url with <code>@id</code>.
     *
     * @throws MalformedURLException not expected
     */
    @Test
    public void testRepositoryWithID()
        throws MalformedURLException
    {
        final MavenRepositoryURL repositoryURL = new MavenRepositoryURL( "file:some/dir@id=repository1" );
        assertEquals( "Releases enabled", "repository1", repositoryURL.getId());
    }

}