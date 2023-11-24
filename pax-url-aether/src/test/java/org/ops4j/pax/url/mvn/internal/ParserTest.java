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
package org.ops4j.pax.url.mvn.internal;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;

public class ParserTest
{

    @Test( expected = MalformedURLException.class )
    public void constructorWithNullPath()
        throws MalformedURLException
    {
        new Parser( null );
    }

    @Test( expected = MalformedURLException.class )
    public void urlStartingWithRepositorySeparator()
        throws MalformedURLException
    {
        new Parser( "!group" );
    }

    @Test( expected = MalformedURLException.class )
    public void urlEndingWithRepositorySeparator()
        throws MalformedURLException
    {
        new Parser( "http://repository!" );
    }

    @Test( expected = MalformedURLException.class )
    public void urlWithRepositoryAndNoGroup()
        throws MalformedURLException
    {
        new Parser( "http://repository!" );
    }

    @Test( expected = MalformedURLException.class )
    public void urlWithoutRepositoryAndNoGroup()
        throws MalformedURLException
    {
        new Parser( "" );
    }

    @Test( expected = MalformedURLException.class )
    public void urlWithRepositoryAndNoArtifact()
        throws MalformedURLException
    {
        new Parser( "http://repository!group" );
    }

    @Test( expected = MalformedURLException.class )
    public void urlWithoutRepositoryAndNoArtifact()
        throws MalformedURLException
    {
        new Parser( "group" );
    }

    @Test
    public void behaviorSnapshotEnabled()
        throws MalformedURLException
    {
        Parser parser;
        parser = new Parser( "http://repository@id=fake!group/artifact" );
        assertTrue(parser.getRepositoryURL().isSnapshotsEnabled());
        assertTrue(parser.getRepositoryURL().isReleasesEnabled());
    }
    
    @Test
    public void urlWithRepositoryAndGroupArtifact()
        throws MalformedURLException
    {
        Parser parser = new Parser( "http://repository@id=fake!group/artifact" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "LATEST", parser.getVersion() );
        assertEquals( "Type", "jar", parser.getType() );
        assertNull("Classifier", parser.getClassifier());
        assertEquals( "Artifact path", "group/artifact/LATEST/artifact-LATEST.jar", parser.getArtifactPath() );
        assertEquals( "repository", URI.create( "http://repository/" ), parser.getRepositoryURL().getURI() );
    }

    @Test
    public void urlWithoutRepositoryAndGroupArtifact()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "LATEST", parser.getVersion() );
        assertEquals( "Type", "jar", parser.getType() );
        assertNull("Classifier", parser.getClassifier());
        assertEquals( "Artifact path", "group/artifact/LATEST/artifact-LATEST.jar", parser.getArtifactPath() );
        assertNull("repository", parser.getRepositoryURL());
    }

    @Test
    public void urlWithRepositoryAndGroupArtifactVersionType()
        throws MalformedURLException
    {
        Parser parser = new Parser( "http://repository@id=fake!group/artifact/version/type" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "version", parser.getVersion() );
        assertEquals( "Type", "type", parser.getType() );
        assertNull("Classifier", parser.getClassifier());
        assertEquals( "Artifact path", "group/artifact/version/artifact-version.type", parser.getArtifactPath() );
        assertEquals( "repository", URI.create( "http://repository/" ), parser.getRepositoryURL().getURI() );
    }

    @Test
    public void urlWithRepositoryAndGroupArtifactVersionTypeClassifier()
        throws MalformedURLException
    {
        Parser parser = new Parser( "http://repository@id=fake!group/artifact/version/type/classifier" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "version", parser.getVersion() );
        assertEquals( "Type", "type", parser.getType() );
        assertEquals( "Classifier", "classifier", parser.getClassifier() );
        assertEquals( "Artifact path", "group/artifact/version/artifact-version-classifier.type",
                      parser.getArtifactPath()
        );
        assertEquals( "repository", URI.create( "http://repository/" ), parser.getRepositoryURL().getURI() );
    }

    @Test
    public void urlWithoutRepositoryAndGroupArtifactVersionType()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version/type" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "version", parser.getVersion() );
        assertEquals( "Type", "type", parser.getType() );
        assertNull("Classifier", parser.getClassifier());
        assertEquals( "Artifact path", "group/artifact/version/artifact-version.type", parser.getArtifactPath() );
        assertNull("repository", parser.getRepositoryURL());
    }

    @Test
    public void urlWithoutRepositoryAndGroupArtifactVersionTypeClassifier()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version/type/classifier" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "version", parser.getVersion() );
        assertEquals( "Type", "type", parser.getType() );
        assertEquals( "Classifier", "classifier", parser.getClassifier() );
        assertEquals( "Artifact path", "group/artifact/version/artifact-version-classifier.type",
                      parser.getArtifactPath()
        );
        assertNull("repository", parser.getRepositoryURL());
    }

    @Test
    public void urlWithoutRepositoryAndGroupArtifactVersionClassifier()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version//classifier" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "version", parser.getVersion() );
        assertEquals( "Type", "jar", parser.getType() );
        assertEquals( "Classifier", "classifier", parser.getClassifier() );
        assertEquals( "Artifact path", "group/artifact/version/artifact-version-classifier.jar",
                      parser.getArtifactPath()
        );
        assertNull("repository", parser.getRepositoryURL());
    }

    @Test
    public void urlWithoutRepositoryAndGroupArtifactTypeClassifier()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact//type/classifier" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "LATEST", parser.getVersion() );
        assertEquals( "Type", "type", parser.getType() );
        assertEquals( "Classifier", "classifier", parser.getClassifier() );
        assertEquals( "Artifact path", "group/artifact/LATEST/artifact-LATEST-classifier.type",
                      parser.getArtifactPath()
        );
        assertNull("repository", parser.getRepositoryURL());
    }

    @Test
    public void urlWithoutRepositoryAndGroupArtifactClassifier()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact///classifier" );
        assertEquals( "Group", "group", parser.getGroup() );
        assertEquals( "Artifact", "artifact", parser.getArtifact() );
        assertEquals( "Version", "LATEST", parser.getVersion() );
        assertEquals( "Type", "jar", parser.getType() );
        assertEquals( "Classifier", "classifier", parser.getClassifier() );
        assertEquals( "Artifact path", "group/artifact/LATEST/artifact-LATEST-classifier.jar",
                      parser.getArtifactPath()
        );
        assertNull("repository", parser.getRepositoryURL());
    }

    @Test
    public void urlWithJarRepository()
        throws MalformedURLException
    {
        Parser parser = new Parser( "jar:http://repository/repository.jar!/@id=fake!group/artifact/0.1.0" );
        assertEquals( "Artifact path", "group/artifact/0.1.0/artifact-0.1.0.jar", parser.getArtifactPath() );
        assertEquals( "repository",
                URI.create( "jar:http://repository/repository.jar!/" ),
                      parser.getRepositoryURL().getURI()
        );
    }

    @Test
       public void trailingSpace()
           throws MalformedURLException
       {
           Parser parser = new Parser( " http://repository/repository@id=fake!group/artifact/0.1.0" );
           assertEquals( "Artifact path", "group/artifact/0.1.0/artifact-0.1.0.jar", parser.getArtifactPath() );
           assertEquals( "repository",
                   URI.create( "http://repository/repository/" ),
                         parser.getRepositoryURL().getURI()
           );
       }

    @Test
    public void snapshotPath()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version-SNAPSHOT" );
        assertEquals( "Artifact snapshot path", "group/artifact/version-SNAPSHOT/artifact-version-timestamp-build.jar",
                      parser.getSnapshotPath( "version-SNAPSHOT", "timestamp", "build" )
        );
    }

    @Test
    public void artifactPathWithVersion()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version" );
        assertEquals( "Artifact path", "group/artifact/version2/artifact-version2.jar",
                      parser.getArtifactPath( "version2" )
        );
    }

    @Test
    public void versionMetadataPath()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version" );
        assertEquals( "Version metadata path", "group/artifact/version2/maven-metadata.xml",
                      parser.getVersionMetadataPath( "version2" )
        );
    }

    @Test
    public void artifactMetadataPath()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version" );
        assertEquals( "Artifact metadata path", "group/artifact/maven-metadata.xml",
                      parser.getArtifactMetdataPath()
        );
    }

    @Test
    public void artifactLocalMetadataPath()
        throws MalformedURLException
    {
        Parser parser = new Parser( "group/artifact/version" );
        assertEquals( "Artifact local metadata path", "group/artifact/maven-metadata-local.xml",
                      parser.getArtifactLocalMetdataPath()
        );
    }

    @Test
    public void justURLsAndURIs() throws URISyntaxException, MalformedURLException {
        String fullMvnURL1 = "mvn:group/artifact/version";
        String fullMvnURL2 = "mvn:http://localhost/nexus!group/artifact/version";

        assertTrue(URI.create(fullMvnURL1).isAbsolute()); // because it has scheme
        assertTrue(URI.create(fullMvnURL2).isAbsolute()); // because it has scheme
        assertTrue(URI.create(fullMvnURL1).isOpaque()); // because there's no "/" after scheme
        assertTrue(URI.create(fullMvnURL2).isOpaque()); // because there's no "/" after scheme
        assertEquals("group/artifact/version", URI.create(fullMvnURL1).getSchemeSpecificPart());
        assertEquals("http://localhost/nexus!group/artifact/version", URI.create(fullMvnURL2).getSchemeSpecificPart());

        assertFalse(URI.create("file:/tmp").isOpaque());
        assertEquals("/tmp@", URI.create("file:/tmp%40").getSchemeSpecificPart());
        assertEquals("/tmp%40", URI.create("file:/tmp%40").getRawSchemeSpecificPart());

        URI uri = URI.create("https://scott:tiger@example.com/maven");
        assertEquals("scott:tiger@example.com", uri.getRawAuthority());
        assertEquals("scott:tiger", uri.getRawUserInfo());

        // URI.create(String) == new URI(String) + exception handling
        URI uri1 = URI.create("/%2F/");
        assertEquals("///", uri1.getPath());
        assertEquals("/%2F/", uri1.getRawPath());
        URI uri2 = new URI("/%2F/");
        assertEquals("///", uri2.getPath());
        assertEquals("/%2F/", uri2.getRawPath());
        URI uri3 = new URI(null, null, null, -1, "/%2F/", null, null);
        assertEquals("/%2F/", uri3.getPath());
        assertEquals("/%252F/", uri3.getRawPath());

        // encoding character: replacing it with escaped octets
        // quoting: encoding, but for illegal characters
        //
        // escaped octets may appear in userInfo, path, query and fragment, each
        // component has own set of illegal characters that need to be quoted

        // single-arg constructor requires that any illegal character is already quoted
        try {
            URI uri4 = URI.create("https://host/maven%");
            fail("% has to be quoted");
        } catch (IllegalArgumentException expected) {
        }
        URI uri5 = URI.create("https://h@o@st/maven");
        // two "@"s is an error within authority and it resets already collected user info / host
        assertNull(uri5.getUserInfo());
        assertNull(uri5.getHost());
        assertEquals("h@o@st", uri5.getAuthority());
        // multi-arg constructor quote illegal characters automatically
        // "%" is always quoted as "%25"
        // user info can't include "@", so it is quoted as "%40"
        URI uri6 = new URI("https", ";:&=+$,sco:tt@x:tige%3Ar@123", "host", -1, "/#maven%", "", null);
        assertEquals("/#maven%", uri6.getPath());
        assertEquals("/%23maven%25", uri6.getRawPath());
        // user:password in userInfo part of authority is not mandated (quite opposite - it's not recommended)
        // by RFC2396. ":" is not interpretted by URI implementation itself, it's up to the user to interpret it
        assertEquals(";:&=+$,sco:tt@x:tige%3Ar@123", uri6.getUserInfo());
        assertEquals(";:&=+$,sco:tt%40x:tige%253Ar%40123", uri6.getRawUserInfo());

        // java.net.URI.quote() is called in several places:
        // - fragment
        // - path
        // - query
        // - authority

        // URL is like more specific URI with attached handler (for URL.openConnection() / URL.openStream())
        // URL uses java.net.URLStreamHandler.parseURL() to parse the value
        // URL uses java.net.URI.Parser.parse()
        URL url1 = new URL("file://user:password@host:1234/etc/passwd%25?a=b#root");
        URI url1uri = url1.toURI();
        assertEquals("user:password", url1.getUserInfo());
        assertEquals("file", url1uri.getScheme());
        assertEquals("user:password@host:1234", url1uri.getAuthority());
        assertEquals("user:password", url1uri.getUserInfo());
        assertEquals("host", url1uri.getHost());
        assertEquals(1234, url1uri.getPort());
        assertEquals("/etc/passwd%", url1uri.getPath());
        assertEquals("/etc/passwd%25", url1uri.getRawPath());
        assertEquals("a=b", url1uri.getQuery());
        assertEquals("root", url1uri.getFragment());
    }

    @Test
    public void fullMvnURI() throws Exception {
        // even if "split" option makes sense only for file:-based local/default repositories, it is always parsed
        Parser p = new Parser("https://sco%3Att:ti%25ger@example.com/tmp/repository@snapshots@id=my-repo@split!commons-pax/commons-pax-url/1.0/xml/features");

        assertEquals("commons-pax/commons-pax-url/1.0/commons-pax-url-1.0-features.xml", p.getArtifactPath());
        assertEquals("commons-pax", p.getGroup());
        assertEquals("commons-pax-url", p.getArtifact());
        assertEquals("1.0", p.getVersion());
        assertEquals("xml", p.getType());
        assertEquals("features", p.getClassifier());

        MavenRepositoryURL repositoryURL = p.getRepositoryURL();
        assertEquals("my-repo", repositoryURL.getId());
        assertTrue(repositoryURL.isSplit());
        assertEquals("installed", repositoryURL.getSplitLocalPrefix());
        assertEquals("sco:tt", repositoryURL.getUsername());
        assertEquals("ti%ger", new String(repositoryURL.getPassword()));

        assertEquals("", new String(new Parser("https://sco%3Att:@example.com!com.example/xyz").getRepositoryURL().getPassword()));
        assertNull(new Parser("https://sco%3Att@example.com!com.example/xyz").getRepositoryURL().getPassword());
    }

}
