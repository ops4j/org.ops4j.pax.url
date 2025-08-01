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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.standalonestatic.ProfileSelectorSupplier;
import eu.maveniverse.maven.mima.runtime.standalonestatic.SettingsBuilderSupplier;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.ActivationFile;
import org.apache.maven.settings.ActivationOS;
import org.apache.maven.settings.ActivationProperty;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

/**
 * Service Configuration implementation.
 * 
 * @author Alin Dreghiciu
 * @author Guillaume Nodet
 * @see MavenConfiguration
 * @since August 11, 2007
 */
public class MavenConfigurationImpl implements MavenConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenConfigurationImpl.class);

    /**
     * Configuration PID. Cannot be null or empty.
     */
    private final String m_pid;

    /**
     * The character that should be the first character in repositories property in order to be
     * appended with the repositories from settings.xml.
     */
    private final static String REPOSITORIES_APPEND_SIGN = "+";

    /**
     * Repositories separator.
     */
    private final static String REPOSITORIES_SEPARATOR = ",";

    private final static String REPOSITORIES_SEPARATOR_SPLIT = "\\s*,\\s*";

    /**
     * Use a default timeout of 5 seconds.
     */
    private final static Integer DEFAULT_TIMEOUT = 5000;

    /**
     * Property resolver. Cannot be null.
     */
    private final PropertyResolver m_propertyResolver;

    /**
     * Built and decrypted settings.
     */
    private Settings settings;

    /**
     * Settings decrypter that can be used each time new settings are set
     */
    private final SettingsDecrypter decrypter;

    /**
     * Map of resolved/cached properties. Properties are stored with their expected type.
     */
    private final Map<String, Object> m_properties = new ConcurrentHashMap<>();

    private static final Object NULL_VALUE = new Object();

    /**
     * Creates a new Maven configuration. Passed {@link PropertyResolver} is immediately used to determine
     * location of {@code settings.xml} file and local repository (in such order, because settings may specify
     * {@code <localRepository>} element).
     * 
     * @param propertyResolver propertyResolver used to resolve properties; mandatory
     * @param pid configuration PID. Set to "" if null.
     */
    public MavenConfigurationImpl(final PropertyResolver propertyResolver, final String pid) {
        NullArgumentException.validateNotNull(propertyResolver, "Property resolver");

        m_pid = pid == null || pid.trim().isEmpty() ? "" : pid.trim() + ".";
        m_propertyResolver = propertyResolver;

        // build settings (not decrypted yet)
        File settingsFile = getSettingsFile();
        try {
            settings = buildSettings(settingsFile, useFallbackRepositories());
        } catch (SettingsBuildingException e) {
            throw new IllegalArgumentException("Can't parse settings.xml file: " + e.getMessage(), e);
        }

        // determine local repository - possibly using a value from settings file
        determineLocalRepository();

        // build security settings (settings-security.xml) to decrypt settings.
        File securitySettingsFile = getSecuritySettingsFile();
        if (securitySettingsFile != null) {
            DefaultPlexusCipher plexusCipher = new DefaultPlexusCipher();
            DefaultSecDispatcher secDispatcher = new DefaultSecDispatcher(
                    plexusCipher,
                    Collections.emptyMap(),
                    securitySettingsFile.getAbsolutePath());

            decrypter = new DefaultSettingsDecrypter(secDispatcher);
            decryptCurrentSettings();
        } else {
            decrypter = null;
        }
    }

    @Override
    public String getPid() {
        return m_pid;
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return m_propertyResolver;
    }

    /**
     * Should the configuration be used at all? If {@link ServiceConstants#REQUIRE_CONFIG_ADMIN_CONFIG} property
     * is present, it is a hint that there should be new configuration (without this property) available soon from
     * different source (in practice - when switchin from default, {@link org.osgi.framework.BundleContext} based
     * configuration to Configuration Admin based configuration (thus the name of the property).
     *
     * @return
     */
    public boolean isValid() {
        return !getProperty(ServiceConstants.REQUIRE_CONFIG_ADMIN_CONFIG, false, Boolean.class);
    }

    @Override
    public boolean isOffline() {
        return getProperty(ServiceConstants.PROPERTY_OFFLINE, false, Boolean.class);
    }

    @Override
    public File getSettingsFile() {
        String key = m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE;
        if (!contains(key)) {
            List<String> fallbacks = new ArrayList<>(4);
            fallbacks.add(m_propertyResolver.get(key));
            fallbacks.add(System.getProperty("user.home") + "/.m2/settings.xml");
            fallbacks.add(System.getProperty("maven.home") + "/conf/settings.xml");
            fallbacks.add(System.getenv("M2_HOME") + "/conf/settings.xml");

            File settingsFile = findFirstAccessibleFile(ServiceConstants.PROPERTY_SETTINGS_FILE, fallbacks, false);
            return set(key, settingsFile);
        }
        return get(key);
    }

    @Override
    public File getSecuritySettingsFile() {
        String key = m_pid + ServiceConstants.PROPERTY_SETTINGS_SECURITY_FILE;
        if (!contains(key)) {
            List<String> fallbacks = new ArrayList<>(2);
            fallbacks.add(m_propertyResolver.get(key));
            fallbacks.add(System.getProperty("user.home") + "/.m2/settings-security.xml");

            File settingsSecurityFile = findFirstAccessibleFile(ServiceConstants.PROPERTY_SETTINGS_SECURITY_FILE, fallbacks, false);
            return set(key, settingsSecurityFile);
        }
        return get(key);
    }

    /**
     * Iterates over locations and returns first file (or directory) that can be accessed.
     *
     * @param fallbacks
     * @param directory
     * @return
     */
    private File findFirstAccessibleFile(String option, List<String> fallbacks, boolean directory) {
        for (String location : fallbacks) {
            File f = safeGetFile(option, location, directory);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    /**
     * If file for {@code path} is accessible, return related {@link File}.
     *
     * @param option if file is not accessible, option indicates the configuration property used for this file
     * @param path
     * @param directory
     * @return
     */
    private File safeGetFile(String option, String path, boolean directory) {
        if (path != null && !path.trim().isEmpty()) {
            path = path.trim().replace('\\', '/');
            path = path.trim().replaceAll("%5C", "/");
            path = path.trim().replaceAll("%5c", "/");
            if (path.startsWith("file:")) {
                URI uri = URI.create(path);
                if (uri.isOpaque()) {
                    // no slash after "file:"
                    path = uri.getSchemeSpecificPart();
                } else {
                    path = uri.getPath();
                }
            }
            File file = new File(path);
            if (directory) {
                // we're looking for location of local repository - it doesn't have to exist, but it can't
                // be an existing file
                if (!file.exists() || (file.canRead() && file.isDirectory())) {
                    return file;
                }
            } else {
                if (file.exists() && file.canRead() && file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }

    @Override
    public String getGlobalUpdatePolicy() {
        return getProperty(ServiceConstants.PROPERTY_GLOBAL_UPDATE_POLICY, null, String.class);
    }

    @Override
    public String getGlobalChecksumPolicy() {
        return getProperty(ServiceConstants.PROPERTY_GLOBAL_CHECKSUM_POLICY, null, String.class);
    }

    /**
     * Returns local repository directory by using the following resolution:<ol>
     *     <li>looks for a configuration property named {@code localRepository}</li>
     *     <li>looks for a framework property/system setting localRepository</li>
     *     <li>looks in {@code settings.xml} (see settings.xml resolution)</li>
     *     <li>looks for system property {@code maven.repo.local} (PAXURL-231)</li>
     *     <li>falls back to {@code ${user.home}/.m2/repository}</li>
     * </ol>
     *
     * {@inheritDoc}
     */
    @Override
    public File getLocalRepository() {
        // should be set in constructor
        // but may be altered if new Settings object is created and added to this configuration
        return get(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY);
    }

    @Override
    public MavenRepositoryURL getLocalMavenRepositoryURL() {
        return get(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY_URL);
    }

    @Override
    public boolean useFallbackRepositories() {
        return getProperty(ServiceConstants.PROPERTY_USE_FALLBACK_REPOSITORIES, true, Boolean.class);
    }

    @Override
    public Integer getTimeout() {
        return getProperty(ServiceConstants.PROPERTY_TIMEOUT, DEFAULT_TIMEOUT, Integer.class);
    }

    @Override
    public boolean getCertificateCheck() {
        return getProperty(ServiceConstants.PROPERTY_CERTIFICATE_CHECK, true, Boolean.class);
    }

    @Override
    public List<MavenRepositoryURL> getDefaultRepositories() throws MalformedURLException {
        String key = m_pid + ServiceConstants.PROPERTY_DEFAULT_REPOSITORIES;
        if (!contains(key)) {
            String defaultRepositoriesProp = m_propertyResolver.get(key);

            final List<MavenRepositoryURL> defaultRepositoriesProperty = new ArrayList<>();
            if (defaultRepositoriesProp != null) {
                String[] repositories = defaultRepositoriesProp.split(REPOSITORIES_SEPARATOR_SPLIT);
                for (String repositoryURL : repositories) {
                    defaultRepositoriesProperty.add(new MavenRepositoryURL(repositoryURL.trim()));
                }
            }
            LOGGER.trace("Using default repositories [" + defaultRepositoriesProperty + "]");
            return set(key, defaultRepositoriesProperty);
        }
        return get(key);
    }

    @Override
    public List<MavenRepositoryURL> getRepositories() throws MalformedURLException {
        String key = m_pid + ServiceConstants.PROPERTY_REPOSITORIES;
        if (!contains(key)) {
            String remoteRepositoriesProp = m_propertyResolver.get(key);

            final List<MavenRepositoryURL> repositoriesFromSettings = new ArrayList<>();
            final List<MavenRepositoryURL> remoteRepositories = new ArrayList<>();

            // get (if needed) repositories from settings.xml
            if (remoteRepositoriesProp == null || remoteRepositoriesProp.trim().startsWith(REPOSITORIES_APPEND_SIGN)) {
                Map<String, Profile> profiles = settings.getProfilesAsMap();
                for (String id : getActiveProfileIDs()) {
                    Profile profile = profiles.get(id);
                    if (profile == null) {
                        continue;
                    }
                    for (org.apache.maven.settings.Repository repo : profile.getRepositories()) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(repo.getUrl());
                        builder.append(ServiceConstants.SEPARATOR_OPTIONS);
                        builder.append(ServiceConstants.OPTION_ID);
                        builder.append("=");
                        builder.append(repo.getId());

                        if (repo.getReleases() != null) {
                            if (!repo.getReleases().isEnabled()) {
                                builder.append(ServiceConstants.SEPARATOR_OPTIONS);
                                builder.append(ServiceConstants.OPTION_DISALLOW_RELEASES);
                            }
                            addPolicy(builder, repo.getReleases().getUpdatePolicy(), ServiceConstants.OPTION_RELEASES_UPDATE);
                            addPolicy(builder, repo.getReleases().getChecksumPolicy(), ServiceConstants.OPTION_RELEASES_CHECKSUM);
                        }
                        if (repo.getSnapshots() != null) {
                            if (repo.getSnapshots().isEnabled()) {
                                builder.append(ServiceConstants.SEPARATOR_OPTIONS);
                                builder.append(ServiceConstants.OPTION_ALLOW_SNAPSHOTS);
                            }
                            addPolicy(builder, repo.getSnapshots().getUpdatePolicy(), ServiceConstants.OPTION_SNAPSHOTS_UPDATE);
                            addPolicy(builder, repo.getSnapshots().getChecksumPolicy(), ServiceConstants.OPTION_SNAPSHOTS_CHECKSUM);
                        }

                        repositoriesFromSettings.add(new MavenRepositoryURL(builder.toString()));
                    }
                }
            }

            // repositories from the property itself
            if (remoteRepositoriesProp != null) {
                if (remoteRepositoriesProp.trim().startsWith(REPOSITORIES_APPEND_SIGN)) {
                    remoteRepositoriesProp = remoteRepositoriesProp.trim().substring(1);
                }
                if (!remoteRepositoriesProp.trim().isEmpty()) {
                    String[] repositories = remoteRepositoriesProp.split(REPOSITORIES_SEPARATOR_SPLIT);
                    for (String repositoryURL : repositories) {
                        remoteRepositories.add(new MavenRepositoryURL(repositoryURL.trim()));
                    }
                }
            }
            remoteRepositories.addAll(repositoriesFromSettings);

            LOGGER.trace("Using remote repositories [" + remoteRepositories + "]");
            return set(key, remoteRepositories);
        }
        return get(key);
    }

    private void addPolicy(StringBuilder builder, String policy, String option) {
        if (policy != null && !policy.isEmpty()) {
            builder.append(ServiceConstants.SEPARATOR_OPTIONS);
            builder.append(option);
            builder.append("=");
            builder.append(policy);
        }
    }

    /**
     * Returns active profile names from current settings
     * @return
     */
    private Collection<String> getActiveProfileIDs() {
        ProfileSelector selector = new ProfileSelectorSupplier().get();

        // see eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport.convertToSettingsProfile()
        List<Profile> settingsProfiles = settings.getProfiles();
        List<org.apache.maven.model.Profile> profiles = new ArrayList<>(settingsProfiles.size());
        settingsProfiles.forEach(p -> {
            org.apache.maven.model.Profile mp = new org.apache.maven.model.Profile();
            mp.setSource(org.apache.maven.model.Profile.SOURCE_SETTINGS);
            mp.setId(p.getId());

            Activation a = p.getActivation();
            if (a != null) {
                org.apache.maven.model.Activation ma = new org.apache.maven.model.Activation();
                mp.setActivation(ma);
                ma.setActiveByDefault(a.isActiveByDefault());

                ActivationFile af = a.getFile();
                if (af != null) {
                    org.apache.maven.model.ActivationFile maf = new org.apache.maven.model.ActivationFile();
                    maf.setExists(af.getExists());
                    maf.setMissing(af.getMissing());
                    ma.setFile(maf);
                }
                String aj = a.getJdk();
                if (aj != null) {
                    ma.setJdk(aj);
                }
                ActivationProperty ap = a.getProperty();
                if (ap != null) {
                    org.apache.maven.model.ActivationProperty map = new org.apache.maven.model.ActivationProperty();
                    map.setName(ap.getName());
                    map.setValue(ap.getValue());
                    ma.setProperty(map);
                }
                ActivationOS ao = a.getOs();
                if (ao != null) {
                    org.apache.maven.model.ActivationOS mao = new org.apache.maven.model.ActivationOS();
                    mao.setName(ao.getName());
                    mao.setVersion(ao.getVersion());
                    mao.setArch(ao.getArch());
                    mao.setFamily(ao.getFamily());
                    ma.setOs(mao);
                }
            }
            // skip other profile data, because we care only about activation here

            profiles.add(mp);
        });

        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setActiveProfileIds(settings.getActiveProfiles());
        // we can't specify it via settings.xml - it's for `-P-xxx` mvn invocation
        context.setInactiveProfileIds(Collections.emptyList());

        // we need properties, but we have only PropertyResolver. It delegates to PID and BundleContext
        // properties, but let's stick to system properties only
        context.setSystemProperties(new Properties(System.getProperties()));

        ModelProblemCollector problemCollector = new ModelProblemCollector() {
            @Override
            public void add(ModelProblemCollectorRequest req) {
                LOGGER.warn(req.getMessage() + " " + req.getLocation());
            }
        };
        List<org.apache.maven.model.Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problemCollector);
        Set<String> profileNames = new LinkedHashSet<>();
        activeProfiles.forEach(ap -> profileNames.add(ap.getId()));

        return profileNames;
    }

    /**
     * Constructs {@link Settings} object, possibly adding fallback Maven Central
     * repository and configure global mirror if specified via env/sys properties.
     *
     * @param settingsFile
     * @param useFallbackRepositories
     * @return
     */
    private Settings buildSettings(File settingsFile, boolean useFallbackRepositories) throws SettingsBuildingException {
        Settings settings;
        if (settingsFile == null) {
            settings = new Settings();
        } else {
            // use Maven API (through MiMa high level class)
            SettingsBuilder builder = new SettingsBuilderSupplier().get();
            SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile(settingsFile);
            SettingsBuildingResult result = builder.build(request);
            settings = result.getEffectiveSettings();
        }

        if (useFallbackRepositories) {
            // Add Maven Central
            Profile fallbackProfile = new Profile();
            fallbackProfile.setId("fallback");

            Repository central = new Repository();
            central.setId("central");
            central.setUrl(ContextOverrides.CENTRAL.getUrl());
            central.setReleases(new RepositoryPolicy());
            central.getReleases().setEnabled(true);
            central.getReleases().setUpdatePolicy(org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER);
            central.getReleases().setChecksumPolicy(org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN);
            central.setSnapshots(new RepositoryPolicy());
            central.getSnapshots().setEnabled(false);
            fallbackProfile.setRepositories(Collections.singletonList(central));

            settings.addProfile(fallbackProfile);
            settings.addActiveProfile("fallback");
        }

        // PAXURL-351 - external configuration of _single_ Mirror for all repositories
        String mirror = System.getenv(ServiceConstants.ENV_MAVEN_MIRROR_URL);
        if (mirror == null || mirror.trim().isEmpty()) {
            mirror = System.getProperty(ServiceConstants.SYS_MAVEN_MIRROR_URL, "");
        }
        if (mirror != null && !mirror.trim().isEmpty()) {
            String[] mirrorData = mirror.trim().split("::");
            String id = "mirror";
            String url;
            if (mirrorData.length > 1) {
                id = mirrorData[0];
                url = mirrorData[1];
            } else {
                url = mirrorData[0];
            }
            Mirror m = new Mirror();
            m.setId(id);
            m.setUrl(url);
            m.setLayout("default");
            m.setMirrorOf("*");
            settings.setMirrors(Collections.singletonList(m));
            LOGGER.info("Setting global Maven mirror to {}", url);
        }

        return settings;
    }

    private void decryptCurrentSettings() {
        SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(settings);
        SettingsDecryptionResult result = decrypter.decrypt(request);
        // only servers and proxies are decrypted
        settings.setServers(result.getServers());
        settings.setProxies(result.getProxies());
    }

    private void determineLocalRepository() {
        String key = m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY;

        String localRepositoryProperty = m_propertyResolver.get(key);
        int at = localRepositoryProperty == null ? -1 : localRepositoryProperty.indexOf("@");
        String localRepositoryLocation = at > 0 ? localRepositoryProperty.substring(0, at) : localRepositoryProperty;

        List<String> fallbacks = new ArrayList<>(4);
        fallbacks.add(localRepositoryLocation);
        fallbacks.add(settings.getLocalRepository());
        fallbacks.add(System.getProperty("maven.repo.local"));
        fallbacks.add(System.getProperty("user.home") + "/.m2/repository");
        fallbacks.add(System.getProperty("java.io.tmpdir") + "/.m2/repository");

        File localRepository = findFirstAccessibleFile(ServiceConstants.PROPERTY_LOCAL_REPOSITORY, fallbacks, true);
        // set the determined local repository back into settings (to make things consistent)
        if (localRepository != null) {
            settings.setLocalRepository(localRepository.getAbsolutePath());
            set(key, localRepository);
        }

        MavenRepositoryURL localRepositoryURL = null;
        File fromPid = safeGetFile(ServiceConstants.PROPERTY_LOCAL_REPOSITORY, localRepositoryLocation, true);
        if (fromPid != null) {
            // we can get some hints from the URI if there's @ sign - but this is handled by MavenRepositoryURL
            // constructor anyway
            if (at > 0) {
                localRepositoryProperty = fromPid.toURI().toString() + localRepositoryProperty.substring(at);
            } else {
                localRepositoryProperty = fromPid.toURI().toString() + "@id=local";
            }
            try {
                localRepositoryURL = new MavenRepositoryURL(localRepositoryProperty);
            } catch (MalformedURLException unexpected) {
                LOGGER.warn(unexpected.getMessage(), unexpected);
            }
        } else {
            // no hints, just URL
            try {
                localRepositoryURL = localRepository == null ? null : new MavenRepositoryURL(localRepository.toURI().toString() + "@id=local");
            } catch (MalformedURLException unexpected) {
                LOGGER.warn(unexpected.getMessage(), unexpected);
            }
        }
        set(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY_URL, localRepositoryURL);
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
        determineLocalRepository();
        if (decrypter != null) {
            decryptCurrentSettings();
        }
    }

    // ---- Property related methods

    @Override
    public <T> T getProperty(String name, T defaultValue, Class<T> clazz) {
        if (!contains(m_pid + name)) {
            String value = m_propertyResolver.get(m_pid + name);
            return set(m_pid + name, value == null ? defaultValue : convert(value, clazz));
        }
        return get(m_pid + name);
    }

    /**
     * Supports String to [ Integer, Long, String, Boolean ] conversion
     * @param value
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> T convert(String value, Class<T> clazz) {
        if (String.class == clazz) {
            return (T) value;
        }
        if (Integer.class == clazz) {
            return (T) Integer.valueOf(value);
        }
        if (Long.class == clazz) {
            return (T) Long.valueOf(value);
        }
        if (Boolean.class == clazz || Boolean.TYPE == clazz) {
            return (T) Boolean.valueOf("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value));
        }
        throw new IllegalArgumentException("Can't convert \"" + value + "\" to " + clazz + ".");
    }

    /**
     * Returns true if the the property was set.
     *
     * @param propertyName name of the property
     *
     * @return true if property is set
     */
    public boolean contains( final String propertyName )
    {
        return m_properties.containsKey( propertyName );
    }

    /**
     * Sets a property.
     *
     * @param propertyName  name of the property to set
     * @param propertyValue value of the property to set
     *
     * @return the value of property set (fluent api)
     */
    public <T> T set( final String propertyName, final T propertyValue )
    {
        m_properties.put( propertyName, propertyValue != null ? propertyValue : NULL_VALUE );
        return propertyValue;
    }

    /**
     * Returns the property by name.
     *
     * @param propertyName name of the property
     *
     * @return property value
     */
    @SuppressWarnings( "unchecked" )
    public <T> T get( final String propertyName )
    {
        Object v = m_properties.get( propertyName );
        return v != NULL_VALUE ? (T) v : null;
    }

}
