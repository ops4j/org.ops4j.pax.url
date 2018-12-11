/*
 * Copyright (C) 2010 Toni Menzel
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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simply playing with mvn api.
 */
public class AetherLocalRepositoryTest {

    private static Logger LOG = LoggerFactory.getLogger(AetherLocalRepositoryTest.class);

    @Test
    public void resolveArtifactWithChangedLocalRepository()
            throws DependencyCollectionException, ArtifactResolutionException, IOException {
        MavenConfigurationImpl config = getConfig();
        config.getLocalRepository().getFile().getName().endsWith(".dir");

        File f = File.createTempFile("aethertest", ".overriden", new File("target"));
        f.delete();
        f.mkdirs();

        String previousMavenRepoLocal = System.getProperty("maven.repo.local");
        try {
            System.setProperty("maven.repo.local", f.getAbsolutePath());
            config = getConfig();
            config.getLocalRepository().getFile().getName().endsWith(".overriden");
        } finally {
            if (previousMavenRepoLocal != null) {
                System.setProperty("maven.repo.local", previousMavenRepoLocal);
            } else {
                System.getProperties().remove("maven.repo.local");
            }
        }
    }

    private Settings getSettings() {
        Settings settings = new Settings();
        settings.setLocalRepository(getCache().toURI().toASCIIString());
        Profile centralProfile = new Profile();
        centralProfile.setId("central");
        Repository central = new Repository();
        central.setId("central");
        central.setUrl("http://repo1.maven.org/maven2");
        centralProfile.addRepository(central);
        settings.addProfile(centralProfile);
        settings.addActiveProfile("central");
        return settings;
    }

    private MavenConfigurationImpl getConfig() {
        Properties p = new Properties();
        MavenConfigurationImpl config = new MavenConfigurationImpl(new PropertiesPropertyResolver(p), ServiceConstants.PID);
        config.setSettings(getSettings());
        return config;
    }

    private File getCache() {
        File base = new File("target");
        base.mkdir();
        try {
            File f = File.createTempFile("aethertest", ".dir", base);
            f.delete();
            f.mkdirs();
            LOG.info("Caching" + " to " + f.getAbsolutePath());
            return f;
        } catch (IOException exc) {
            throw new AssertionError("cannot create cache", exc);
        }
    }

}
