package org.ops4j.pax.url.mvn.internal;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.FileSettingsSource;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;

public class MavenSettingsReader {

	public static Settings readSettings() {
		DefaultSettingsBuilderFactory sbf = new DefaultSettingsBuilderFactory();
		DefaultSettingsBuilder settingsBuilder = sbf.newInstance();
		DefaultSettingsBuildingRequest dsbr = new DefaultSettingsBuildingRequest();
		dsbr.setUserSettingsSource(new FileSettingsSource(new File(System
				.getProperty("user.home") + "/.m2/settings.xml")));

		try {
			SettingsBuildingResult settingsResult = settingsBuilder.build(dsbr);
			if (!settingsResult.getProblems().isEmpty()) {
				throw new RuntimeException("Errors reading m2 settings");
			}

			Settings settings = settingsResult.getEffectiveSettings();

			DefaultPlexusCipher cipher = new DefaultPlexusCipher();
			DefaultSecDispatcher dispatcher = createSecDispatcher(cipher);

			DefaultSettingsDecrypter decrypter = createSettingsDecrypter(dispatcher);

			SettingsDecryptionResult decryptRes = decrypter
					.decrypt(new DefaultSettingsDecryptionRequest(settings));

			if (!decryptRes.getProblems().isEmpty()) {
				throw new RuntimeException(
						"Errors decrypting m2 settings password");

			}

			List<Server> servers = decryptRes.getServers();

			settings.setServers(servers);
			return settings;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static DefaultSecDispatcher createSecDispatcher(
			DefaultPlexusCipher cipher) throws NoSuchFieldException,
			IllegalAccessException {
		DefaultSecDispatcher dispatcher = new DefaultSecDispatcher();

		Field cipherField = DefaultSecDispatcher.class
				.getDeclaredField("_cipher");
		cipherField.setAccessible(true);
		cipherField.set(dispatcher, cipher);
		cipherField.setAccessible(false);

		Field decryptorsField = DefaultSecDispatcher.class
				.getDeclaredField("_decryptors");
		decryptorsField.setAccessible(true);
		decryptorsField.set(dispatcher,
				new HashMap<String, PasswordDecryptor>());
		cipherField.setAccessible(false);

		dispatcher.setConfigurationFile(System.getProperty("user.home")
				+ "/.m2/settings-security.xml");
		return dispatcher;
	}

	private static DefaultSettingsDecrypter createSettingsDecrypter(
			DefaultSecDispatcher dispatcher) throws NoSuchFieldException,
			IllegalAccessException {
		DefaultSettingsDecrypter decrypter = new DefaultSettingsDecrypter();
		Field secDispatcherField = DefaultSettingsDecrypter.class
				.getDeclaredField("securityDispatcher");
		secDispatcherField.setAccessible(true);
		secDispatcherField.set(decrypter, dispatcher);
		secDispatcherField.setAccessible(false);
		return decrypter;
	}
}
