package org.ops4j.pax.url.mvn.internal.config;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;

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
        assertEquals( new File( "somewhere/localrepository/" ), mavenRepo.getFile() );
        assertEquals( new URL( spec ), mavenRepo.getURL() );

        spec = "file:repository1\\";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( new File( "repository1/" ), mavenRepo.getFile() );
        assertEquals( new URL( spec ), mavenRepo.getURL() );

        spec = "file:somewhere/localrepository%5C";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( new URL( spec + "/" ), mavenRepo.getURL() );

        spec = "file:repository1%5C";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        assertEquals( new URL( spec + "/" ), mavenRepo.getURL() );

        spec = "file:r%C3%A9positories%20/r%C3%A9pository1";
        mavenRepo = new MavenRepositoryURL( spec + "@id=repository1" );
        File expected = new File( "répositories /répository1/" );
        assertEquals( expected, mavenRepo.getFile() );

    }

}
