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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.ops4j.io.StreamUtils;
import org.ops4j.lang.NullArgumentException;

/**
 * Resources to jar adapter.
 *
 * @author Alin Dreghiciu
 * @since 1.1.0, August 31, 2009
 */
class VirtualJar
{

    private final Iterable<Resource> m_resources;
    private final URL m_manifest;

    VirtualJar( final URL manifest,
                final Iterable<Resource> resources )
    {
        NullArgumentException.validateNotNull( resources, "Resources" );
        m_resources = resources;
        m_manifest = manifest;
    }

    InputStream inputStream()
        throws IOException
    {
        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream pis = new PipedInputStream( pos );
        new Thread()
        {

            public void run()
            {
                JarOutputStream jos = null;
                try
                {
                    if( m_manifest == null )
                    {
                        jos = new JarOutputStream( pos );
                    }
                    else
                    {
                        jos = new JarOutputStream( pos, new Manifest( m_manifest.openStream() ) );
                    }
                    for( Resource resource : m_resources )
                    {
                        if( !"META-INF/MANIFEST.MF".equals( resource.path() ) )
                        {
                            jos.putNextEntry( new JarEntry( resource.path() ) );
                            StreamUtils.copyStream( resource.url().openStream(), jos, false );
                        }
                    }
                }

                catch( IOException e )
                {
                    throw new RuntimeException( "Could not process resources", e );
                }
                finally
                {
                    try
                    {
                        if( jos != null )
                        {
                            jos.close();
                        }
                    }
                    catch( Exception ignore )
                    {
                        //  ignore
                    }
                }
            }
        }.start();

        return pis;
    }

}