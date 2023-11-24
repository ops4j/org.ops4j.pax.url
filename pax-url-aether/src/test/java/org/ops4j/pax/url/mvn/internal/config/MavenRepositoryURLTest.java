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

    }

}
