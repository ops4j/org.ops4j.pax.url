/*
 * Copyright (C) 2013 Andrei Pozolotin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify snapshot updates.
 */
public class GlobalUpdatePolicyTest {

	static Logger LOG = LoggerFactory.getLogger(GlobalUpdatePolicyTest.class);

	/**
	 * Test project used to deploy snapshots.
	 */
	static final String GROUP = "org.ops4j.pax.url";
	static final String ARTIFACT = "pax-url-aether-test";
	static final String VERSION = "1.0.0-SNAPSHOT";
	static final String TYPE = "jar";

	/**
	 * Location of test project sources.
	 */
	static final File BASEDIR = new File(System.getProperty("user.dir"));
	static final File PROJECT = new File(BASEDIR, ARTIFACT);

	/**
	 * Command to deploy snapshots, using alternative repository.
	 */
	static final String COMMAND = //
	"mvn deploy --define maven.repo.local=test-repo";

	private void mavenDeploy() throws Exception {
		UnitHelp.process(COMMAND, PROJECT);
	}

	@Test
	public void resolveArtifact() throws Exception {

		final AetherBasedResolver resolver = new AetherBasedResolver(
				UnitHelp.getUserConfig());

		LOG.info("init");

		final long time0 = System.currentTimeMillis();

		final long time1;
		{
			mavenDeploy();

			final File file = //
			resolver.resolveFile(GROUP, ARTIFACT, "", TYPE, VERSION);

			time1 = file.lastModified();
		}

		LOG.info("deploy 1");

		final long time2;
		{
			mavenDeploy();

			final File file = //
			resolver.resolveFile(GROUP, ARTIFACT, "", TYPE, VERSION);

			time2 = file.lastModified();
		}

		LOG.info("deploy 2");

		assertTrue(time0 > 0);
		assertTrue(time1 > 0);
		assertTrue(time2 > 0);

		assertTrue(time1 > time0);
		assertTrue(time2 > time0);

		assertTrue(time2 > time1);

		LOG.info("done");

	}

}
