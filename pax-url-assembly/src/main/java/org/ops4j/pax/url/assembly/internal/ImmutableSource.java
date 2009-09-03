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

import java.util.Arrays;
import java.util.regex.Pattern;
import org.ops4j.io.ListerUtils;
import org.ops4j.lang.NullArgumentException;

/**
 * A source of resources.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
public class ImmutableSource
    implements Source
{

    /**
     * Base source directory.
     */
    private final String m_path;
    /**
     * List of includes.
     */
    private final Pattern[] m_includes;
    /**
     * List of excludes.
     */
    private final Pattern[] m_excludes;

    /**
     * Constructor.
     *
     * @param path path encoded source (path[!/[include|!exclude][,[include|!exclude]]])
     */
    public ImmutableSource( final String path,
                            final Pattern[] includes,
                            final Pattern[] excludes )
    {
        NullArgumentException.validateNotEmpty( path, true, "Path" );

        m_path = path;
        m_includes = ( includes == null || includes.length == 0 ) ? new Pattern[]{ ListerUtils.parseFilter( "**" ) }
                                                                  : includes;
        m_excludes = ( excludes == null ) ? new Pattern[0] : excludes;
    }

    /**
     * {@inheritDoc}
     */
    public String path()
    {
        return m_path;
    }

    /**
     * {@inheritDoc}
     */
    public Pattern[] includes()
    {
        return m_includes;
    }

    /**
     * {@inheritDoc}
     */
    public Pattern[] excludes()
    {
        return m_excludes;
    }

    @Override
    public String toString()
    {
        return String.format(
            "%s includes(%s) excludes(%s)", m_path, Arrays.deepToString( m_includes ), Arrays.deepToString( m_excludes )
        );
    }

}