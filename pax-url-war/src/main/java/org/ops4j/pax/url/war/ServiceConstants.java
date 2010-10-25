/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.url.war;

/**
 * An enumeration of constants related to war url handler.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 13, 2008
 */
public interface ServiceConstants
{

    /**
     * Service PID used for configuration.
     */
    static final String PID = "org.ops4j.pax.url.war";
    /**
     * The "war" protocol name.
     */
    public static final String PROTOCOL_WAR = "war";
    /**
     * The "war-i" protocol name.
     */
    public static final String PROTOCOL_WAR_INSTRUCTIONS = "war-i";
    /**
     * The "warref:" protocol name.
     */
    public static final String PROTOCOL_WAR_REFERENCE = "warref";
    /**
     * The "webbundle:" protocol name.
     */
    public static final String PROTOCOL_WEB_BUNDLE = "webbundle";    
    /**
     * Certificate check configuration property name.
     */
    static final String PROPERTY_CERTIFICATE_CHECK = PID + ".certificateCheck";
    /**
     * Import pax-logging packages to ease the deployment of WAR files
     * when pax-logging is present.
     */
    static final String PROPERTY_IMPORT_PAXLOGGING_PACKAGES = PID + ".importPaxLoggingPackages";
    /**
     * URI of the war file to be processed.
     */
    static final String INSTR_WAR_URL = "WAR-URL";
    /**
     * Bundle classpath.
     */
    static final String INSTR_BUNDLE_CLASSPATH = "Bundle-ClassPath";

}