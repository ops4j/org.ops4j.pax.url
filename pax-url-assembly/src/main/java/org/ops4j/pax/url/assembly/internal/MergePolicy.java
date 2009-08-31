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

import java.util.Set;

/**
 * Policy to use while merging assembly resources.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
public interface MergePolicy
{

    /**
     * First wins merge policy.
     */
    static MergePolicy FIRST = new MergePolicy()
    {
        /**
         * Adds resource only if not already present in set.
         *
         * {@inheritDoc}
         */
        public void addResource( Resource resource, Set<Resource> resources )
        {
            if( resources.contains( resource ) )
            {
                return;
            }
            resources.add( resource );
        }

    };

    /**
     * Last wins merge policy.
     */
    static MergePolicy LAST = new MergePolicy()
    {
        /**
         * Replace resource in set.
         *
         * {@inheritDoc}
         */
        public void addResource( Resource resource, Set<Resource> resources )
        {
            resources.add( resource );
        }

    };

    /**
     * Adds resource to set of resources based on type of policy.
     *
     * @param resource  resource to add
     * @param resources set when the resource should be added
     */
    public void addResource( Resource resource, Set<Resource> resources );

}