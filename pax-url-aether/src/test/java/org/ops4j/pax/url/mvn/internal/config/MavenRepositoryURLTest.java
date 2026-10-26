/*
 * Copyright 2023 OPS4J.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MavenRepositoryURLTest
{
    @Test
    public void testMavenRepositoryURL() throws MalformedURLException
    {
        File localrepo = new File( "my dir/repository" ).getAbsoluteFile();
        String uri = localrepo.toURI().toASCIIString();
        MavenRepositoryURL mavenRepo = new MavenRepositoryURL( uri + "@id=repository1" );
        assertEquals( localrepo, mavenRepo.getFile() );

        localrepo = new File( "myédir/repository" ).getAbsoluteFile();
        uri = localrepo.toURI().toASCIIString();
        mavenRepo = new MavenRepositoryURL( uri + "@id=repository1" );
        assertEquals( localrepo, mavenRepo.getFile() );

        String spec = "file:repository1/@id=repository1";
        mavenRepo = new MavenRepositoryURL( spec );
        assertEquals( new File( "repository1/" ), mavenRepo.getFile() );

        spec = "file:repositories/repository1/@id=repository1";
        mavenRepo = new MavenRepositoryURL( spec );
        assertEquals( new File( "repositories/repository1/" ), mavenRepo.getFile() );

        spec = "file:somewhere/localrepository\\";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( new File( "somewhere/localrepository" ), mavenRepo.getFile() );
        assertEquals( URI.create( "file:somewhere/localrepository/" ), mavenRepo.getURI() );

        spec = "file:repository1%5C";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( new File( "repository1/" ), mavenRepo.getFile() );
        assertEquals( URI.create( "file:repository1/" ), mavenRepo.getURI() );

        spec = "file:somewhere/localrepository%5C";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( URI.create( "file:somewhere/localrepository/" ), mavenRepo.getURI() );

        spec = "file:repository1%5C";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( URI.create( "file:repository1/" ), mavenRepo.getURI() );

        spec = "file:r%C3%A9positories%20/r%C3%A9pository1";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        File expected = new File( "répositories /répository1/" );
        assertEquals( expected, mavenRepo.getFile() );

        spec = "file:r%C3%A9positories /r%C3%A9pository1";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        expected = new File( "répositories /répository1/" );
        assertEquals( expected, mavenRepo.getFile() );
    }

    @Test
    public void testCredentialsAreMasked() throws MalformedURLException
    {
        MavenRepositoryURL mavenRepo = new MavenRepositoryURL( "https://user:secret@repo.example.com/maven2@id=repository1" );
        assertEquals( "https://***@repo.example.com/maven2/ {id=repository1}", mavenRepo.toString() );
        assertEquals( URI.create( "https://user:secret@repo.example.com/maven2/" ), mavenRepo.getURI() );

        mavenRepo = new MavenRepositoryURL( "https://user%40example.com:secret@repo.example.com/maven2@id=repository1" );
        assertEquals( "https://***@repo.example.com/maven2/ {id=repository1}", mavenRepo.toString() );

        mavenRepo = new MavenRepositoryURL( "https://user:p/ss@repo.example.com/maven2@id=repository1" );
        assertEquals( "https://***@repo.example.com/maven2/ {id=repository1}", mavenRepo.toString() );

        mavenRepo = new MavenRepositoryURL( "https://repo.example.com/maven2@snapshots@id=repository1" );
        assertEquals( "https://repo.example.com/maven2/ {id=repository1}", mavenRepo.toString() );
    }

    @Test
    public void testInvalidSpecIsMasked() throws MalformedURLException
    {
        try
        {
            new MavenRepositoryURL( "https://user:p%zz@repo.example.com/maven2@id=repository1" );
            fail( "IllegalArgumentException expected" );
        }
        catch( IllegalArgumentException e )
        {
            assertEquals( "Invalid repository spec https://***@repo.example.com/maven2/: Malformed escape pair", e.getMessage() );
        }
    }

    @Test
    public void testMaskUserInfo()
    {
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://user:secret@repo.example.com/maven2/" ) );
        assertEquals( "https://***@repo.example.com:8443/maven2/", MavenRepositoryURL.maskUserInfo( "https://user@repo.example.com:8443/maven2/" ) );
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://user:p@ss@repo.example.com/maven2/" ) );
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://user:p/ss@repo.example.com/maven2/" ) );
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://user:p?ss@repo.example.com/maven2/" ) );
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://user:p#ss@repo.example.com/maven2/" ) );
        assertEquals( "https://***@[::1]:8443/maven2/?key=value", MavenRepositoryURL.maskUserInfo( "https://user:secret@[::1]:8443/maven2/?key=value" ) );
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://user:p%40ss@repo.example.com/maven2/" ) );
        assertEquals( "https://***@repo.example.com/content/@scope/", MavenRepositoryURL.maskUserInfo( "https://user:secret@repo.example.com/content/@scope/" ) );
        assertEquals( "https://***@repo.example.com/maven2@snapshots@id=repository1", MavenRepositoryURL.maskUserInfo( "https://user:secret@repo.example.com/maven2@snapshots@id=repository1" ) );
        assertEquals( "https://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://me@example.com:secret@repo.example.com/maven2/" ) );
        assertEquals( "ftp://***@repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "ftp://user:secret@repo.example.com/maven2/" ) );
        assertEquals( "file://***@uncshare/c:/users/repository/", MavenRepositoryURL.maskUserInfo( "file://user:secret@uncshare/c:/users/repository/" ) );
        assertEquals( "file://***@uncshare/c:\\users\\repository\\", MavenRepositoryURL.maskUserInfo( "file://user:secret@uncshare/c:\\users\\repository\\" ) );

        assertEquals( "https://repo.example.com/maven2/", MavenRepositoryURL.maskUserInfo( "https://repo.example.com/maven2/" ) );
        assertEquals( "https://repo.example.com/maven2@snapshots@id=repository1", MavenRepositoryURL.maskUserInfo( "https://repo.example.com/maven2@snapshots@id=repository1" ) );
        assertEquals( "https://repo.example.com/content/@scope/", MavenRepositoryURL.maskUserInfo( "https://repo.example.com/content/@scope/" ) );
        assertEquals( "https://repo.example.com/maven2/?user=a@b", MavenRepositoryURL.maskUserInfo( "https://repo.example.com/maven2/?user=a@b" ) );
        assertEquals( "file:///C:/Users/john@corp/repository/", MavenRepositoryURL.maskUserInfo( "file:///C:/Users/john@corp/repository/" ) );
        assertEquals( "file:repository1/", MavenRepositoryURL.maskUserInfo( "file:repository1/" ) );
        assertEquals( "file://uncshare/c:\\users\\repository\\", MavenRepositoryURL.maskUserInfo( "file://uncshare/c:\\users\\repository\\" ) );
        assertEquals( "file:///C:/Program Files/repository@snapshots", MavenRepositoryURL.maskUserInfo( "file:///C:/Program Files/repository@snapshots" ) );
        assertEquals( "file:///C:\\Users\\john@corp\\repository@snapshots", MavenRepositoryURL.maskUserInfo( "file:///C:\\Users\\john@corp\\repository@snapshots" ) );
    }

}
