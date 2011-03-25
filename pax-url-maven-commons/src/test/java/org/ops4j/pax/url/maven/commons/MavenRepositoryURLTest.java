package org.ops4j.pax.url.maven.commons;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.Test;

public class MavenRepositoryURLTest {

	@Test
	public void testMavenRepositoryURL() throws MalformedURLException {
		File localrepo = new File("my dir/repository").getAbsoluteFile();
		String uri = localrepo.toURI().toASCIIString();
		MavenRepositoryURL mavenRepo = new MavenRepositoryURL(uri);
		assertEquals(localrepo, mavenRepo.getFile());
		
		localrepo = new File("myédir/repository").getAbsoluteFile();
		uri = localrepo.toURI().toASCIIString();
		mavenRepo = new MavenRepositoryURL(uri);
		assertEquals(localrepo, mavenRepo.getFile());
		
		String spec = "file:repository1/";
		mavenRepo = new MavenRepositoryURL(spec);
		assertEquals(new File("repository1/"), mavenRepo.getFile());
		
		
		spec = "file:repositories/repository1/";
		mavenRepo = new MavenRepositoryURL(spec);
		assertEquals(new File("repositories/repository1/"), mavenRepo.getFile());
		
		spec = "file:r%C3%A9positories%20/r%C3%A9pository1";
		mavenRepo = new MavenRepositoryURL(spec);
		File expected = new File("répositories /répository1/");
		assertEquals(expected, mavenRepo.getFile());
		
	}

}
