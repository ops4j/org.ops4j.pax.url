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
	}

}
