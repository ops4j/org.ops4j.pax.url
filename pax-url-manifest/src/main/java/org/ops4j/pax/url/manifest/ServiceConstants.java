/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.url.manifest;

/**
 * An enumeration of constants related to "manifest" url handler.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, June 24, 2009
 */
public interface ServiceConstants
{

    /**
     * Service PID used for configuration.
     */
    static final String PID = "org.ops4j.pax.url.manifest";
    /**
     * The "manifest" protocol name.
     */
    public static final String PROTOCOL_MANIFEST = "manifest";
    /**
     * The "mf" protocol name.
     */
    public static final String PROTOCOL_MF = "mf";    

    /**
     * Certificate check configuration property name.
     */
    static final String PROPERTY_CERTIFICATE_CHECK = PID + ".certificateCheck";

}