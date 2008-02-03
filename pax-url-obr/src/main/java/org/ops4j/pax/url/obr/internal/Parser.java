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
package org.ops4j.pax.url.obr.internal;

import java.net.MalformedURLException;
import org.ops4j.lang.NullArgumentException;

/**
 * Parser for obr: protocol url.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, February 01, 2008
 */
class Parser
{

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "obr:<bundle-symbolic-name>['/'<bundle-version>]";
    /**
     * OBR filter.
     */
    private final String m_filter;

    /**
     * Creates a new url protocol parser.
     *
     * @param path the path part of the url (without starting wrap:)
     *
     * @throws java.net.MalformedURLException if provided path does not comply to expected syntax or has malformed urls
     *                                        or contains values that doe not pass an OSGi filter validation
     * @throws NullArgumentException          if filter validator is null
     */
    public Parser( final String path,
                   final FilterValidator filterValidator )
        throws MalformedURLException
    {
        NullArgumentException.validateNotNull( filterValidator, "Filter validator" );

        if( path == null || path.trim().length() == 0 )
        {
            throw new MalformedURLException( "Path cannot be null or empty. Syntax " + SYNTAX );
        }
        final String[] segments = path.split( "/" );
        if( segments.length > 2 )
        {
            throw new MalformedURLException( "Path canot contain more then one '/'. Syntax  " + SYNTAX );
        }
        final StringBuilder builder = new StringBuilder();
        // add bundle symbolic name filter
        builder.append( "(symbolicname=" ).append( segments[ 0 ] ).append( ")" );
        if( !filterValidator.validate( builder.toString() ) )
        {
            throw new MalformedURLException( "Invalid symbolic name value." );
        }
        // add bundle version filter
        if( segments.length > 1 )
        {
            builder.insert( 0, "(&" ).append( "(version=" ).append( segments[ 1 ] ).append( "))" );
            if( !filterValidator.validate( builder.toString() ) )
            {
                throw new MalformedURLException( "Invalid version value." );
            }
        }
        m_filter = builder.toString();
    }

    /**
     * Getter.
     *
     * @return obr filter part of the url
     */
    public String getFilter()
    {
        return m_filter;
    }

}