/*
 * Copyright (C) 2012 Andrei Pozolotin
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
package amazon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SettingsTest {

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void amazonSettings() throws Exception {

		File file = Util.getTestSettings();

		MavenSettings settings = new MavenSettingsImpl(file.toURI().toURL());

		String repositories = settings.getRepositories();

		assertNotNull("missing repositories", repositories);

		String[] segments = repositories.split(",");

		assertEquals("repository counter", 1, segments.length);

		assertEquals(
				"amazon repository 1",
				"https://User-Agent:magic-token@localhost:11443@id=amazon-server-1",
				segments[0]);

	}

}
