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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.ops4j.lang.NullArgumentException;

/**
 * File based implementation of {@link Resource}.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class FileResource
    implements Resource
{

    /**
     * A parent directory relative to which the path should be calculated.
     */
    private final File m_parent;
    /**
     * Resource file.
     */
    private final File m_file;

    /**
     * Constructor.
     *
     * @param file resource file
     */
    FileResource( final File parent,
                  final File file )
    {
        NullArgumentException.validateNotNull( parent, "Resource parent" );
        NullArgumentException.validateNotNull( file, "Resource file" );
        try
        {
            if( !file.getCanonicalPath().startsWith( parent.getCanonicalPath() ) )
            {
                throw new IllegalArgumentException(
                    String.format(
                        "Specified parent [%s] is not a parent of file [%s]", parent.getPath(), file.getPath()
                    )
                );
            }
        }
        catch( IOException e )
        {
            throw new IllegalArgumentException( "Validation failed due to exception", e );
        }
        m_parent = parent;
        m_file = file;
    }

    /**
     * Returns file canonical path relative to parent.
     *
     * {@inheritDoc}
     */
    public String path()
    {
        try
        {
            return m_file.getCanonicalPath()
                .substring( m_parent.getCanonicalPath().length() + 1 )
                .replace( File.separatorChar, '/' );
        }
        catch( IOException e )
        {
            throw new RuntimeException( String.format( "Cannot determine path for file [%s]", m_file.getPath() ), e );
        }
    }

    /**
     * Returns file url.
     *
     * {@inheritDoc}
     */
    public URL url()
    {
        try
        {
            return m_file.toURL();
        }
        catch( MalformedURLException e )
        {
            // this cannot happen ;)
            throw new RuntimeException( String.format( "Cannot convert file [%s] to an url", m_file.getPath() ), e );
        }
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