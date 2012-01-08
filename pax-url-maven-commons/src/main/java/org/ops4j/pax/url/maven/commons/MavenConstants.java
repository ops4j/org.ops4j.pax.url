/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.url.maven.commons;

/**
 * An enumeration of constants related to maven handler.
 *
 * @author Alin Dreghiciu
 * @since August 26, 2007
 */
public interface MavenConstants {
    /**
     * Warning: use only in framework properties.  If present, do not accept configuration, wait for one without this flag.
     */
    static final String REQUIRE_CONFIG_ADMIN_CONFIG = ".require.config.admin.config";
    /**
     * Certificate check configuration property name.
     */
    static final String PROPERTY_CERTIFICATE_CHECK = ".certificateCheck";
    /**
     * Maven settings file configuration property name.
     */
    static final String PROPERTY_SETTINGS_FILE = ".settings";
    /**
     * LocalRepository configuration property name.
     */
    static final String PROPERTY_LOCAL_REPOSITORY = ".localRepository";
    /**
     * DefaultRepositories configuration property name
     */
    static final String PROPERTY_DEFAULT_REPOSITORIES = ".defaultRepositories";
    /**
     * Repositories configuration property name.
     */
    static final String PROPERTY_REPOSITORIES = ".repositories";
    /**
     * Use fallback repositories switch configuration property name.
     */
    static final String PROPERTY_USE_FALLBACK_REPOSITORIES = ".useFallbackRepositories";
    /**
     * Proxy support configuration property name.
     */
    static final String PROPERTY_PROXY_SUPPORT = ".proxySupport";
    /**
     * Option to mark repository as allowing snapshots.
     */
    static String OPTION_ALLOW_SNAPSHOTS = "snapshots";
    /**
     * Option to mark repository as not allowing releases.
     */
    static String OPTION_DISALLOW_RELEASES = "noreleases";
    /**
     * Options separator in repository url.
     */
    static String SEPARATOR_OPTIONS = "@";
    /**
     * Proxies given via property.
     * Expected layout: http:host=foo,port=8080;https:host=bar,port=9090
     */
    static String PROPERTY_PROXIES = ".proxies";
    /**
     * Default enable proxies or not if property PROPERTY_PROXY_SUPPORT is not set at all.
     */
    static boolean PROPERTY_PROXY_SUPPORT_DEFAULT = true;
    /**
     * segment in repository spec that gives the name of the repo. Crucial for Aether handler.
     */
    static final String OPTION_ID = "id";
}