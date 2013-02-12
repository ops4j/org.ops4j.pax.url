/*
 * Copyright 2013 Andrei Pozolotin
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
package org.ops4j.pax.url.mvn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenConstants;
import org.ops4j.pax.url.maven.commons.MavenSettings;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

public class UnitHelp {

	public static final Pattern SPACES = Pattern.compile("\\s+");

	/**
	 * Copy streams.
	 */
	public static void copy(InputStream in, OutputStream out) throws Exception {
		while (true) {
			int c = in.read();
			if (c == -1) {
				break;
			}
			out.write((char) c);
		}
	}

	/**
	 * Load settings.xml file.
	 */
	public static MavenConfiguration getConfig(final File settingsFile)
			throws Exception {

		final Properties props = new Properties();

		props.setProperty(ServiceConstants.PID
				+ MavenConstants.PROPERTY_CERTIFICATE_CHECK, "false");

		props.setProperty(ServiceConstants.PID
				+ MavenConstants.PROPERTY_SETTINGS_FILE, settingsFile.toURI()
				.toASCIIString());

		final MavenConfigurationImpl config = new MavenConfigurationImpl(
				new PropertiesPropertyResolver(props), ServiceConstants.PID);

		final MavenSettings settings = new MavenSettingsImpl(settingsFile
				.toURI().toURL());

		config.setSettings(settings);

		return config;

	}

	/**
	 * Load default user configuration form user settings.xml.
	 */
	public static MavenConfiguration getUserConfig() throws Exception {
		return getConfig(getUserSettings());
	}

	/**
	 * Load default user settings.
	 */
	public static File getUserSettings() throws IOException {
		return new File(System.getProperty("user.home"), ".m2/settings.xml");
	}

	/**
	 * Invoke external process and wait for completion.
	 */
	public static void process(final String command, final File directory)
			throws Exception {
		final ProcessBuilder builder = new ProcessBuilder(SPACES.split(command));
		builder.directory(directory);
		builder.redirectErrorStream(true);
		final Process process = builder.start();
		copy(process.getInputStream(), System.out);
		process.waitFor();
	}

	private UnitHelp() {
	}

}
