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
package org.ops4j.pax.url.war.internal;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.commons.resolver.ConfigurationMap;
import org.ops4j.pax.url.commons.resolver.Resolver;
import org.ops4j.pax.url.war.ServiceConstants;

/**
 * War protocol configuration implementation.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 14, 2008
 */
public class ConfigurationImpl
    extends ConfigurationMap
    implements Configuration
{

    /**
     * Property resolver. Cannot be null.
     */
    private final Resolver m_resolver;

    /**
     * Creates a new service configuration.
     *
     * @param resolver resolver used to resolve properties; mandatory
     */
    public ConfigurationImpl( final Resolver resolver )
    {
        NullArgumentException.validateNotNull( resolver, "Property resolver" );
        m_resolver = resolver;
    }

    /**
     * @see Configuration#getCertificateCheck()
     */
    public Boolean getCertificateCheck()
    {
        if( !contains( ServiceConstants.PROPERTY_CERTIFICATE_CHECK ) )
        {
            return set( ServiceConstants.PROPERTY_CERTIFICATE_CHECK,
                        Boolean.valueOf( m_resolver.get( ServiceConstants.PROPERTY_CERTIFICATE_CHECK ) )
            );
        }
        return get( ServiceConstants.PROPERTY_CERTIFICATE_CHECK );
    }

}