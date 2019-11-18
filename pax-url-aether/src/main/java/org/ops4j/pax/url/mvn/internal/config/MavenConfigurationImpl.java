/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright (C) 2014 Guillaume Nodet
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
package org.ops4j.pax.url.mvn.internal.config;

import java.io.File;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Configuration implementation.
 * 
 * @author Alin Dreghiciu
 * @author Guillaume Nodet
 * @see MavenConfiguration
 * @since August 11, 2007
 */
public class MavenConfigurationImpl implements MavenConfiguration {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenConfigurationImpl.class);

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
    private final static String DEFAULT_TIMEOUT = "5000";

    /**
     * Configuration PID. Cannot be null or empty.
     */
    private final String m_pid;
    /**
     * Property resolver. Cannot be null.
     */
    private final PropertyResolver m_propertyResolver;

    private Settings settings;

    /**
     * Creates a new service configuration.
     * 
     * @param propertyResolver
     *            propertyResolver used to resolve properties; mandatory
     * @param pid
     *            configuration PID; mandatory
     */
    public MavenConfigurationImpl(final PropertyResolver propertyResolver, final String pid) {
        NullArgumentException.validateNotNull(propertyResolver, "Property resolver");

        m_pid = pid == null ? "" : pid + ".";
        m_propertyResolver = propertyResolver;
        settings = buildSettings(getLocalRepoPath(propertyResolver), getSettingsFileUrl(),
            useFallbackRepositories());
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return m_propertyResolver;
    }

    public boolean isValid() {
        return m_propertyResolver.get(m_pid + ServiceConstants.REQUIRE_CONFIG_ADMIN_CONFIG) == null;
    }

    /**
     * @see MavenConfiguration#isOffline()
     */
    public boolean isOffline() {
        if (!contains(m_pid + ServiceConstants.PROPERTY_OFFLINE)) {
            return set(
                    m_pid + ServiceConstants.PROPERTY_OFFLINE,
                    Boolean.valueOf(m_propertyResolver.get(m_pid
                            + ServiceConstants.PROPERTY_OFFLINE)));
        }
        return get(m_pid + ServiceConstants.PROPERTY_OFFLINE);
    }

    /**
     * @see MavenConfiguration#getCertificateCheck()
     */
    public Boolean getCertificateCheck() {
        if (!contains(m_pid + ServiceConstants.PROPERTY_CERTIFICATE_CHECK)) {
            return set(
                m_pid + ServiceConstants.PROPERTY_CERTIFICATE_CHECK,
                Boolean.valueOf(m_propertyResolver.get(m_pid
                    + ServiceConstants.PROPERTY_CERTIFICATE_CHECK)));
        }
        return get(m_pid + ServiceConstants.PROPERTY_CERTIFICATE_CHECK);
    }

    /**
     * Returns the URL of settings file. Will try first to use the url as is. If a malformed url
     * encountered then will try to use the url as a file path. If still not valid will throw the
     * original Malformed URL exception.
     * 
     * @see MavenConfiguration#getSettingsFileUrl()
     */
    public URL getSettingsFileUrl() {
        if (!contains(m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE)) {
            String spec = m_propertyResolver.get(m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE);
            if (spec == null) {
                spec = safeGetFile(System.getProperty("user.home") + "/.m2/settings.xml");
            }
            if (spec == null) {
                spec = safeGetFile(System.getProperty("maven.home") + "/conf/settings.xml");
            }
            if (spec == null) {
                spec = safeGetFile(System.getenv("M2_HOME") + "/conf/settings.xml");
            }
            if (spec != null) {
                try {
                    return set(m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE, new URL(spec));
                }
                catch (MalformedURLException e) {
                    File file = new File(spec);
                    if (file.exists()) {
                        try {
                            return set(m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE, file.toURI()
                                .toURL());
                        }
                        catch (MalformedURLException ignore) {
                            // ignore as it usually should not happen since we already have a file
                        }
                    }
                    else {
                        LOGGER
                            .warn("Settings file ["
                                + spec
                                + "] cannot be used and will be skipped (malformed url or file does not exist)");
                        set(m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE, null);
                    }
                }
            }
        }
        return get(m_pid + ServiceConstants.PROPERTY_SETTINGS_FILE);
    }

    private String safeGetFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.canRead() && file.isFile()) {
                try {
                    return file.toURI().toURL().toExternalForm();
                }
                catch (MalformedURLException e) {
                    // Ignore
                }
            }
        }
        return null;
    }

    /**
     * Repository is a comma separated list of repositories to be used. If repository acces requests
     * authentication the user name and password must be specified in the repository url as for
     * example http://user:password@repository.ops4j.org/maven2.<br/>
     * If the repository from 1/2 bellow starts with a plus (+) the option 3 is also used and the
     * repositories from settings.xml will be cummulated.<br/>
     * Repository resolution:<br/>
     * 1. looks for a configuration property named repository;<br/>
     * 2. looks for a framework property/system setting repository;<br/>
     * 3. looks in settings.xml (see settings.xml resolution). in this case all configured
     * repositories will be used including configured user/password. In this case the central
     * repository is also added. Note that the local repository is added as the first repository if
     * exists.
     * 
     * @see MavenConfiguration#getRepositories()
     * @see MavenConfiguration#getLocalRepository()
     */
    public List<MavenRepositoryURL> getDefaultRepositories() throws MalformedURLException {
        if (!contains(m_pid + ServiceConstants.PROPERTY_DEFAULT_REPOSITORIES)) {
            // look for repositories property
            String defaultRepositoriesProp = m_propertyResolver.get(m_pid
                + ServiceConstants.PROPERTY_DEFAULT_REPOSITORIES);
            // build repositories list
            final List<MavenRepositoryURL> defaultRepositoriesProperty = new ArrayList<MavenRepositoryURL>();
            if (defaultRepositoriesProp != null && defaultRepositoriesProp.trim().length() > 0) {
                String[] repositories = defaultRepositoriesProp.split(REPOSITORIES_SEPARATOR_SPLIT);
                for (String repositoryURL : repositories) {
                    defaultRepositoriesProperty.add(new MavenRepositoryURL(repositoryURL.trim()));
                }
            }
            LOGGER.trace("Using default repositories [" + defaultRepositoriesProperty + "]");
            return set(m_pid + ServiceConstants.PROPERTY_DEFAULT_REPOSITORIES,
                defaultRepositoriesProperty);
        }
        return get(m_pid + ServiceConstants.PROPERTY_DEFAULT_REPOSITORIES);
    }

    /**
     * Repository is a comma separated list of repositories to be used. If repository access requests
     * authentication the user name and password must be specified in the repository url as for
     * example http://user:password@repository.ops4j.org/maven2.<br/>
     * If the repository from 1/2 bellow starts with a plus (+) the option 3 is also used and the
     * repositories from settings.xml will be cummulated.<br/>
     * Repository resolution:<br/>
     * 1. looks for a configuration property named repository;<br/>
     * 2. looks for a framework property/system setting repository;<br/>
     * 3. looks in settings.xml (see settings.xml resolution). in this case all configured
     * repositories will be used including configured user/password. In this case the central
     * repository is also added. Note that the local repository is added as the first repository if
     * exists.
     * 
     * @see MavenConfiguration#getRepositories()
     * @see MavenConfiguration#getLocalRepository()
     */
    public List<MavenRepositoryURL> getRepositories() throws MalformedURLException {
        if (!contains(m_pid + ServiceConstants.PROPERTY_REPOSITORIES)) {
            // look for repositories property
            String repositoriesProp = m_propertyResolver.get(m_pid
                + ServiceConstants.PROPERTY_REPOSITORIES);
            // if not set or starting with a plus (+) get repositories from settings xml
            if ((repositoriesProp == null || repositoriesProp.startsWith(REPOSITORIES_APPEND_SIGN))
                && settings != null) {

                String init = (repositoriesProp == null) ? "" : repositoriesProp.substring(1);
                StringBuilder builder = new StringBuilder(init);
                Map<String, Profile> profiles = settings.getProfilesAsMap();
                for (String activeProfile : getActiveProfiles(true)) {
                    Profile profile = profiles.get(activeProfile);
                    if (profile == null) {
                        continue;
                    }
                    for (org.apache.maven.settings.Repository repo : profile.getRepositories()) {
                        if (builder.length() > 0) {
                            builder.append(REPOSITORIES_SEPARATOR);
                        }
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
                    }
                }
                repositoriesProp = builder.toString();
            }
            // build repositories list
            final List<MavenRepositoryURL> repositoriesProperty = new ArrayList<MavenRepositoryURL>();
            if (m_propertyResolver.get(m_pid + ServiceConstants.PROPERTY_LOCAL_REPO_AS_REMOTE) != null) {
                MavenRepositoryURL localRepository = getDefaultLocalRepository();
                if (localRepository != null) {
                    repositoriesProperty.add(localRepository);
                }
            }
            if (repositoriesProp != null && repositoriesProp.trim().length() > 0) {
                String[] repositories = repositoriesProp.split(REPOSITORIES_SEPARATOR_SPLIT);
                for (String repositoryURL : repositories) {
                    if (!"".equals(repositoryURL.trim())) {
                        repositoriesProperty.add(new MavenRepositoryURL(repositoryURL.trim()));
                    }
                }
            }
            LOGGER.trace("Using remote repositories [" + repositoriesProperty + "]");
            return set(m_pid + ServiceConstants.PROPERTY_REPOSITORIES, repositoriesProperty);
        }
        return get(m_pid + ServiceConstants.PROPERTY_REPOSITORIES);
    }

    /**
     * Returns active profile names from current settings
     * @param alsoActiveByDefault if <code>true</code>, also return these profile names that are
     * <code>&lt;activeByDefault&gt;</code>
     * @return
     */
    private Collection<String> getActiveProfiles(boolean alsoActiveByDefault) {
        Set<String> profileNames = new LinkedHashSet<String>(settings.getActiveProfiles());
        if (alsoActiveByDefault) {
            for (Profile profile : settings.getProfiles()) {
                if (profile.getActivation() != null && profile.getActivation().isActiveByDefault()) {
                    // TODO: check other activations - file/jdk/os/property?
                    profileNames.add(profile.getId());
                }
            }
        }
        return profileNames;
    }

    private void addPolicy(StringBuilder builder, String policy, String option) {
        if (policy != null && !policy.isEmpty()) {
            builder.append(ServiceConstants.SEPARATOR_OPTIONS);
            builder.append(option);
            builder.append("=");
            builder.append(policy);
        }
    }

    public String getGlobalUpdatePolicy() {
        final String propertyName = m_pid + ServiceConstants.PROPERTY_GLOBAL_UPDATE_POLICY;
        if (contains(propertyName)) {
            return get(propertyName);
        }
        final String propertyValue = m_propertyResolver.get(propertyName);
        if (propertyValue != null) {
            set(propertyName, propertyValue);
            return propertyValue;
        }
        return null;
    }

    public String getGlobalChecksumPolicy() {
        final String propertyName = m_pid + ServiceConstants.PROPERTY_GLOBAL_CHECKSUM_POLICY;
        if (contains(propertyName)) {
            return get(propertyName);
        }
        final String propertyValue = m_propertyResolver.get(propertyName);
        if (propertyValue != null) {
            set(propertyName, propertyValue);
            return propertyValue;
        }
        return null;
    }

    /**
     * Resolves local repository directory by using the following resolution:<br/>
     * 1. looks for a configuration property named {@code localRepository};<br/>
     * 2. looks for a framework property/system setting localRepository;<br/>
     * 3. looks in settings.xml (see settings.xml resolution);<br/>
     * 4. looks for system property {@code maven.repo.local} (PAXURL-231);<br/>
     * 5. falls back to ${user.home}/.m2/repository.
     * 
     * @see MavenConfiguration#getLocalRepository()
     */
    public MavenRepositoryURL getLocalRepository() {
        if (!contains(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY)) {
            // look for a local repository property
            String spec = m_propertyResolver.get(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY);
            // if not set get local repository from maven settings
            if (spec == null && settings != null) {
                spec = settings.getLocalRepository();
            }
            // check -Dmaven.repo.local property - useful for batch runs within external maven build
            // that accept maven.repo.local as alternative local repository
            if (spec == null) {
                spec = System.getProperty("maven.repo.local");
                if (spec != null) {
                    if (!new File(spec).isDirectory()) {
                        LOGGER.warn("Can't use maven.repo.local=" + spec + ". Location invalid.");
                        spec = null;
                    }
                }
            }
            if (spec == null) {
                spec = System.getProperty("user.home") + "/.m2/repository";
            }
            if (!spec.toLowerCase().contains("@snapshots")) {
                spec += "@snapshots";
            }
            spec += "@id=local";
            // check if we have a valid url
            try {
                return set(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY,
                    new MavenRepositoryURL(spec));
            }
            catch (MalformedURLException e) {
                // maybe is just a file?
                try {
                    return set(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY,
                        new MavenRepositoryURL(new File(spec).toURI().toASCIIString()));
                }
                catch (MalformedURLException ignore) {
                    LOGGER.warn("Local repository [" + spec
                        + "] cannot be used and will be skipped");
                    return set(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY, null);
                }

            }
        }
        return get(m_pid + ServiceConstants.PROPERTY_LOCAL_REPOSITORY);
    }

    public MavenRepositoryURL getDefaultLocalRepository() {
        if (settings != null) {
            String spec = settings.getLocalRepository();
            if (spec == null) {
                spec = System.getProperty("user.home") + "/.m2/repository";
            }
            if (!spec.toLowerCase().contains("@snapshots")) {
                spec += "@snapshots";
            }
            spec += "@id=defaultlocal";
            // check if we have a valid url
            try {
                return new MavenRepositoryURL(spec);
            }
            catch (MalformedURLException e) {
                // maybe is just a file?
                try {
                    return new MavenRepositoryURL(new File(spec).toURI().toASCIIString());
                }
                catch (MalformedURLException ignore) {
                    LOGGER.warn("Local repository [" + spec
                        + "] cannot be used and will be skipped");
                    return null;
                }

            }
        }
        return null;
    }

    public Integer getTimeout() {
        if (!contains(m_pid + ServiceConstants.PROPERTY_TIMEOUT)) {
            String timeout = m_propertyResolver.get(m_pid + ServiceConstants.PROPERTY_TIMEOUT);
            return set(m_pid + ServiceConstants.PROPERTY_TIMEOUT,
                Integer.valueOf(timeout == null ? DEFAULT_TIMEOUT : timeout));
        }
        return get(m_pid + ServiceConstants.PROPERTY_TIMEOUT);
    }

    /**
     * {@inheritDoc}
     */
    public Boolean useFallbackRepositories() {
        if (!contains(m_pid + ServiceConstants.PROPERTY_USE_FALLBACK_REPOSITORIES)) {
            String useFallbackRepoProp = m_propertyResolver.get(m_pid
                + ServiceConstants.PROPERTY_USE_FALLBACK_REPOSITORIES);
            return set(m_pid + ServiceConstants.PROPERTY_USE_FALLBACK_REPOSITORIES,
                Boolean.valueOf(useFallbackRepoProp == null ? "true" : useFallbackRepoProp));
        }
        return get(m_pid + ServiceConstants.PROPERTY_USE_FALLBACK_REPOSITORIES);
    }

    /**
     * Enables the proxy server for a given URL.
     * 
     * @deprecated This method has side-effects and is only used in the "old" resolver.
     */
    @Deprecated
    public void enableProxy(URL url) {
        final String protocol = url.getProtocol();

        Map<String, String> proxyDetails = getProxySettings(url.getProtocol()).get(protocol);
        if (proxyDetails != null) {
            LOGGER.trace("Enabling proxy [" + proxyDetails + "]");

            final String user = proxyDetails.get("user");
            final String pass = proxyDetails.get("pass");

            Authenticator.setDefault(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass.toCharArray());
                }
            });

            System.setProperty(protocol + ".proxyHost", proxyDetails.get("host"));
            System.setProperty(protocol + ".proxyPort", proxyDetails.get("port"));

            System.setProperty(protocol + ".nonProxyHosts", proxyDetails.get("nonProxyHosts"));

            set(m_pid + ServiceConstants.PROPERTY_PROXY_SUPPORT, protocol);
        }
    }

    private boolean isProtocolSupportEnabled(String... protocols) {
        final String proxySupport = m_propertyResolver.get(m_pid
            + ServiceConstants.PROPERTY_PROXY_SUPPORT);
        if (proxySupport == null) {
            return ServiceConstants.PROPERTY_PROXY_SUPPORT_DEFAULT;
        }

        // simple cases:
        if ("true".equalsIgnoreCase(proxySupport)) {
            return true;
        }
        if ("false".equalsIgnoreCase(proxySupport)) {
            return false;
        }

        // giving no protocols to test against, default to true.
        if (protocols.length == 0) {
            return true;
        }

        // differentiate by protocol:
        for (String protocol : protocols) {
            if (proxySupport.contains(protocol)) {
                return true;
            }
        }
        // not in list appearingly.
        return false;
    }

    public Map<String, Map<String, String>> getProxySettings(String... protocols) {
        Map<String, Map<String, String>> pr = new HashMap<String, Map<String, String>>();

        if (isProtocolSupportEnabled(protocols)) {

            parseSystemWideProxySettings(pr);
            parseProxiesFromProperty(
                m_propertyResolver.get(m_pid + ServiceConstants.PROPERTY_PROXIES), pr);

            // if( pr.isEmpty() ) {
            // if( m_settings == null ) { return Collections.emptyMap(); }
            //
            // return m_settings.getProxySettings();
            // }
        }
        return pr;
    }

    private void parseSystemWideProxySettings(Map<String, Map<String, String>> pr) {
        String httpHost = m_propertyResolver.get("http.proxyHost");
        String httpPort = m_propertyResolver.get("http.proxyPort");
        String httpnonProxyHosts = m_propertyResolver.get("http.nonProxyHosts");

        if (httpHost != null) {
            parseProxiesFromProperty("http:host=" + httpHost + ",port=" + httpPort
                + ",nonProxyHosts=" + httpnonProxyHosts, pr);
        }
    }

    // example: http:host=foo,port=8080;https:host=bar,port=9090
    private void parseProxiesFromProperty(String proxySettings, Map<String, Map<String, String>> pr) {
        // TODO maybe make the parsing more clever via regex ;) Or not.
        try {
            if (proxySettings != null) {
                String[] protocols = proxySettings.split(";");

                for (String protocolSection : protocols) {
                    String[] section = protocolSection.split(":");
                    String protocolName = section[0];
                    Map<String, String> keyvalue = new HashMap<String, String>();
                    // set some defaults:
                    keyvalue.put("protocol", protocolName);
                    keyvalue.put("nonProxyHosts", "");
                    keyvalue.put("host", "localhost");
                    keyvalue.put("port", "80");

                    for (String keyvalueList : section[1].split(",")) {
                        String[] kv = keyvalueList.split("=");
                        String key = kv[0];
                        String value = kv[1];
                        keyvalue.put(key, value);
                    }
                    pr.put(protocolName, keyvalue);
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(
                "Proxy setting is set to "
                    + proxySettings
                    + ". But it should have this format: <protocol>:<key>=<value>,<key=value>;protocol:<key>=<value>,..");
        }
    }

    private String getLocalRepoPath(PropertyResolver props) {
        return props.get(ServiceConstants.PID + "." + ServiceConstants.PROPERTY_LOCAL_REPOSITORY);
    }

    private Settings buildSettings(String localRepoPath, URL settingsPath, boolean useFallbackRepositories) {
        Settings settings;
        if (settingsPath == null) {
            settings = new Settings();
        }
        else {
            DefaultSettingsBuilderFactory factory = new DefaultSettingsBuilderFactory();
            DefaultSettingsBuilder builder = factory.newInstance();
            SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            try {
                if (settingsPath.toURI().isOpaque()) {
                    request.setUserSettingsFile(new File(settingsPath.getPath()));
                } else {
                    request.setUserSettingsFile(new File(settingsPath.toURI()));
                }
            } catch (URISyntaxException e) {
                // should never happens because it is returned by safeGetFile
            }
            try {
                SettingsBuildingResult result = builder.build(request);
                settings = result.getEffectiveSettings();
            }
            catch (SettingsBuildingException exc) {
                throw new AssertionError("cannot build settings", exc);
            }

        }
        if (useFallbackRepositories) {
            Profile fallbackProfile = new Profile();
            Repository central = new Repository();
            central.setId("central");
            central.setUrl("https://repo1.maven.org/maven2");
            fallbackProfile.setId("fallback");
            fallbackProfile.setRepositories(Arrays.asList(central));
            settings.addProfile(fallbackProfile);
            settings.addActiveProfile("fallback");
        }
        if (localRepoPath != null) {
            settings.setLocalRepository(localRepoPath);
        }
        // PAXURL-351 - external configuration of _single_ Mirror for all repositories
        String mirror = System.getenv(ServiceConstants.PROPERTY_MAVEN_MIRROR_URL_ENV);
        if (mirror == null || mirror.trim().equals("")) {
            mirror = System.getProperty(ServiceConstants.PROPERTY_MAVEN_MIRROR_URL_SYS, "");
        }
        if (mirror != null && !mirror.trim().equals("")) {
            String[] mirrorData = mirror.split("::");
            String id = "mirror";
            String url = null;
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
            LOGGER.debug("Setting global Maven mirror to " + url);
        }

        return settings;
    }

    public Map<String, Map<String, String>> getMirrors() {
        // DO support mirrors via properties (just like we do for proxies.
        // if( m_settings == null ) { return Collections.emptyMap(); }
        // return m_settings.getMirrorSettings();
        return Collections.emptyMap();
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public String getSecuritySettings() {
        String key = m_pid + ServiceConstants.PROPERTY_SECURITY;
        if (!contains(key)) {
            String spec = m_propertyResolver.get(key);
            if (spec == null) {
                spec = new File(System.getProperty("user.home"), ".m2/settings-security.xml")
                    .getPath();
            }
            return set(key, spec);
        }
        return get(key);
    }

    @Override
    public <T> T getProperty(String name, T defaultValue, Class<T> clazz) {
        if (!contains(m_pid + name)) {
            String value = m_propertyResolver.get(m_pid + name);
            return set(m_pid + name, value == null ? defaultValue : convert(value, clazz));
        }
        return get(m_pid + name);
    }

    @Override
    public String getPid() {
        return m_pid;
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
        if (Boolean.class == clazz) {
            return (T) Boolean.valueOf("true".equals(value));
        }
        throw new IllegalArgumentException("Can't convert \"" + value + "\" to " + clazz + ".");
    }

    /**
     * Map of properties.
     */
    private final Map<String, Object> m_properties = new ConcurrentHashMap<>();

    private static final Object NULL_VALUE = new Object();

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
