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
package org.ops4j.pax.url.assembly.internal;

import java.net.URL;
import org.ops4j.lang.NullArgumentException;

/**
 * A resource from a zip/jar file.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class JarResource
    implements Resource
{

    /**
     * Resource URL.
     */
    private final URL m_url;

    /**
     * Constructor.
     *
     * @param url resource url
     */
    JarResource( final URL url )
    {
        NullArgumentException.validateNotNull( url, "Resource url" );
        if( !"jar".equals( url.getProtocol() )
            && !url.toExternalForm().contains( "!/" ) )
        {
            throw new IllegalArgumentException( String.format( "URL [%s] is not referring to a jar resource", url ) );
        }
        m_url = url;
    }

    /**
     * Returns file canonical path relative to parent.
     *
     * {@inheritDoc}
     */
    public String path()
    {
        final String externalForm = m_url.toExternalForm();
        return externalForm.substring( externalForm.lastIndexOf( "!/" ) + 2 );
    }

    /**
     * Returns file url.
     *
     * {@inheritDoc}
     */
    public URL url()
    {
        return m_url;
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( !( o instanceof Resource ) )
        {
            return false;
        }

        Resource that = (Resource) o;

        return path().equals( that.path() );
    }

    @Override
    public int hashCode()
    {
        return path().hashCode();
    }

    @Override
    public String toString()
    {
        return path();
    }

}