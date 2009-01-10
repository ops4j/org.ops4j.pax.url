/*
 * Copyright 2008 Toni Menzel.
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
package org.ops4j.pax.url.dir.internal.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import org.osgi.framework.Constants;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.bnd.BndUtils;
import org.ops4j.pax.url.dir.internal.ResourceLocator;

/**
 * Responsible for creating the on-the fly testing probe.
 *
 * @author Toni Menzel (tonit)
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since May 29, 2008
 */
public class BundleBuilder
{

    private ResourceLocator m_finder;

    private Properties m_refs;

    /**
     * Constructor.
     *
     * @param ref    name of test class
     * @param finder locator that gathers all resources that have to be inside the test probe
     */
    public BundleBuilder( final Properties ref,
                          final ResourceLocator finder )
    {
        NullArgumentException.validateNotNull( ref, "ref" );
        NullArgumentException.validateNotNull( finder, "finder" );

        m_finder = finder;
        m_refs = ref;

    }

    /**
     * {@inheritDoc}
     */
    public InputStream build()
    {
        try
        {
            // 1. create a basic jar with all classes in it..
            final PipedOutputStream pout = new PipedOutputStream();
            PipedInputStream fis = new PipedInputStream( pout );
            new Thread()
            {

                public void run()
                {
                    JarOutputStream jos;
                    try
                    {
                        jos = new DuplicateAwareJarOutputStream( pout );
                        m_finder.write( jos );
                        jos.close();
                    }

                    catch( IOException e )
                    {
                        //throw new RuntimeException( e );
                    }
                    finally
                    {
                        try
                        {
                            pout.close();
                        }
                        catch( Exception e )
                        {
                            //  throw new TestExecutionException( "Cannot close builder stream ??", e );
                        }
                    }
                }
            }.start();

            // TODO set args on BndUtils
            if( m_refs.getProperty( Constants.BUNDLE_SYMBOLICNAME ) == null )
            {
                m_refs.setProperty( Constants.BUNDLE_SYMBOLICNAME, "BuiltByDirUrlHandler" );
            }
            InputStream result = BndUtils.createBundle( fis, m_refs, m_finder.toString() );
            fis.close();
            pout.close();
            return result;
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }


}
