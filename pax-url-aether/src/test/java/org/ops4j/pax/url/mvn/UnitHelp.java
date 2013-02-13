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

/**
 * Provide unit test utilities.
 */
public class UnitHelp {

	/**
	 * Pattern for non printable characters.
	 */
	public static final Pattern SPACES = Pattern.compile("\\s+");

	/**
	 * Copy streams.
	 */
	public static void copy(final InputStream in, final OutputStream out)
			throws Exception {
		final byte[] array = new byte[8 * 1024];
		int size = 0;
		while ((size = in.read(array)) >= 0) {
			out.write(array, 0, size);
		}
	}

	/**
	 * Load settings.xml file and apply custom properties.
	 */
	public static MavenConfiguration getConfig(final File settingsFile,
			final Properties props) throws Exception {

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
	 * Discover maven home from executable on PATH, using conventions.
	 */
	public static File getMavenHome() throws Exception {
		final String command;
		switch (OS.current()) {
		case LINUX:
		case MAC:
			command = "mvn";
			break;
		case WINDOWS:
			command = "mvn.bat";
			break;
		default:
			throw new IllegalStateException("invalid o/s");
		}
		String pathVar = System.getenv("PATH");
		String[] pathArray = pathVar.split(File.pathSeparator);
		for (String path : pathArray) {
			File file = new File(path, command);
			if (file.exists() && file.isFile() && file.canExecute()) {
				/** unwrap symbolic links */
				File exec = file.getCanonicalFile();
				/** assume ${maven.home}/bin/exec convention */
				File home = exec.getParentFile().getParentFile();
				return home;
			}
		}
		throw new IllegalStateException("Maven home not found.");
	}

	/**
	 * Load default user configuration form user settings.xml with custom
	 * properties.
	 */
	public static MavenConfiguration getUserConfig(final Properties props)
			throws Exception {
		return getConfig(getUserSettings(), props);
	}

	/**
	 * Load default maven settings from user home.
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
