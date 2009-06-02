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
package org.ops4j.pax.url.cache.internal;

import java.io.File;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.cache.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;
import org.ops4j.util.property.PropertyStore;

/**
 * "cache:" protocol configuration implementation.
 *
 * @author Alin Dreghiciu
 * @since 0.6.0, June 02, 2009
 */
public class ConfigurationImpl
    extends PropertyStore
    implements Configuration
{

    /**
     * Property resolver. Cannot be null.
     */
    private final PropertyResolver m_propertyResolver;

    /**
     * Creates a new service configuration.
     *
     * @param propertyResolver propertyResolver used to resolve properties; mandatory
     */
    public ConfigurationImpl( final PropertyResolver propertyResolver )
    {
        NullArgumentException.validateNotNull( propertyResolver, "Property resolver" );
        m_propertyResolver = propertyResolver;
    }

    /**
     * {@inheritDoc}
     */
    public File getRootDirectory()
    {
        if( !contains( ServiceConstants.PROPERTY_ROOT_DIRECTORY ) )
        {
            String rootDirectoryName = m_propertyResolver.get( ServiceConstants.PROPERTY_ROOT_DIRECTORY );
            if( rootDirectoryName == null )
            {
                rootDirectoryName = ServiceConstants.DEFAULT_ROOT_DIRECTORY;
            }
            final File rootDirectory = new File( rootDirectoryName );
            rootDirectory.mkdirs();
            return set( ServiceConstants.PROPERTY_ROOT_DIRECTORY, rootDirectory );
        }
        return get( ServiceConstants.PROPERTY_ROOT_DIRECTORY );
    }
}