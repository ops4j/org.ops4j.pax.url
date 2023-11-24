/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2014 Guillaume Nodet.
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
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.ops4j.util.property.PropertyResolver;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * A configuration interface representing backend dictionary-based configuration (like ConfigurationAdmin PID).
 *
 * @author Alin Dreghiciu
 * @author Guillaume Nodet
 * @since August 11, 2007
 */
public interface MavenConfiguration {

    /**
     * Returns PID for properties used by this configuration. It's used as prefix for properties in
     * associated {@link PropertyResolver}. Defaults to {@link org.ops4j.pax.url.mvn.ServiceConstants#PID}.
     *
     * @return
     */
    String getPid();

    /**
     * Whether {@link DefaultRepositorySystemSession#setOffline(boolean)} should be set to {@code true}.
     *
     * @return
     */
    boolean isOffline();

    /**
     * Returns the location of {@code settings.xml} file used. When {@code null}, no external settings are loaded.
     *
     * @return
     */
    File getSettingsFile();

    /**
     * Returns the location of {@code settings-security.xml} file used. When {@code null}, no external security
     * settings are loaded.
     *
     * @return
     */
    File getSecuritySettingsFile();

    /**
     * <p>Global repository update policy.</p>
     *
     * See {@link org.ops4j.pax.url.mvn.ServiceConstants#PROPERTY_GLOBAL_UPDATE_POLICY}
     *
     * @return repository update policy or null if not set
     */
    String getGlobalUpdatePolicy();

    /**
     * <p>Global repository update policy.</p>
     *
     * See {@link org.ops4j.pax.url.mvn.ServiceConstants#PROPERTY_GLOBAL_CHECKSUM_POLICY}
     *
     * @return repository update policy or null if not set
     */
    String getGlobalChecksumPolicy();

    /**
     * Returns the directory for Maven local repository. Checks context propert/PID, {@code settings.xml},
     * {@code maven.repo.local} system property and falls back to {@code ~/.m2/repository}.
     * Local repository <em>must</em> be specified in order to resolve remote artifacts.
     *
     * @return
     */
    File getLocalRepository();

    /**
     * Returns the {@link MavenRepositoryURL} for Maven local repository. This is the way to get more information
     * about the repository (for example if is it split).
     *
     * @return
     */
    MavenRepositoryURL getLocalMavenRepositoryURL();

    /**
     * Returns true if Maven Central should be added as fallback repository in addition to other configured
     * repositories.Default value is true.
     *
     * @return true if the fallback repositories should be used
     */
    boolean useFallbackRepositories();

    /**
     * Returns a list of default repositories to be searched before any other repositories. These should be file: based
     * repositories. For example Karaf's {@code system/} directory should be configured as <em>default repository</em>.
     *
     * @return a list of default repositories. List can be null or empty if there are not default repositories to
     * be searched.
     */
    List<MavenRepositoryURL> getDefaultRepositories() throws MalformedURLException;

    /**
     * Returns a list of remote repositories to be searched. When resolving artifacts from remote repositories, locally
     * cached version is always created in configured local repository. These repositories may be file: based, but
     * usually external http(s): repositories are used.
     *
     * @return a list of remote repositories. List can be null or empty if there are no repositories to be searched.
     */
    List<MavenRepositoryURL> getRepositories() throws MalformedURLException;

    /**
     * Returns the generic connection/read timeout configured in case the maven artifact is retrieved from a
     * remote location. We can configure the timeouts separately using external {@code settings.xml} as well as
     * dedicated properties like {@link org.ops4j.pax.url.mvn.ServiceConstants#PROPERTY_SOCKET_CONNECTION_TIMEOUT}.
     *
     * @return the timeout in case artifacts are retrieved from a remote location
     * @deprecated use dedicated PID/config/system properties or configure timeouts in {@code settings.xml}
     */
    @Deprecated(since = "3.0")
    Integer getTimeout();

    /**
     * Returns true if the certificate should be checked on SSL connection, false otherwise.
     *
     * @return true if the certificate should be checked
     */
    boolean getCertificateCheck();

    /**
     * Returns a {@link PropertyResolver} backing up this configuration object.
     *
     * @return
     */
    PropertyResolver getPropertyResolver();

    /**
     * Returns generic property by name and type.
     *
     * @param name
     * @param defaultValue
     * @param clazz
     * @return
     */
    <T> T getProperty(String name, T defaultValue, Class<T> clazz);

    /**
     * Returns Maven {@link Settings} object with decrypted values.
     *
     * @return
     */
    Settings getSettings();

}
