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
package org.ops4j.pax.url.war.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In memory repository of byte arrays.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, January 13, 2007
 */
class MemoryRepository
{

    private Map<String, byte[]> m_repository;

    private static final Lock lock = new ReentrantLock();
    private static Integer m_next = 0;

    MemoryRepository()
    {
        m_repository = new HashMap<String, byte[]>();
    }

    public Reference add( final byte[] byteArray )
    {
        return new Reference( byteArray );
    }

    public byte[] get( final String id )
    {
        return m_repository.get( id );
    }

    final protected class Reference
    {

        private final String m_id;

        protected Reference( final byte[] byteArray )
        {
            lock.lock();
            try
            {
                m_id = String.valueOf( m_next++ );
                m_repository.put( m_id, byteArray );
            }
            finally
            {
                lock.unlock();
            }
        }

        public String getId()
        {
            return m_id;
        }

        public void remove()
        {

        }
    }

}