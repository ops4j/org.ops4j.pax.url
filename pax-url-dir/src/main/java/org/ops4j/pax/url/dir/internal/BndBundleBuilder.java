package org.ops4j.pax.url.dir.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.bnd.BndUtils;

/**
 * Responsible for creating the on-the fly testing probe.
 *
 * @author Toni Menzel (tonit)
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since May 29, 2008
 */
public class BndBundleBuilder
{

    private ResourceLocator m_finder;


    private Properties m_refs;

    /**
     * Constructor.
     *
     * @param ref    name of test class
     * @param finder locator that gathers all resources that have to be inside the test probe
     */
    public BndBundleBuilder( final Properties ref,
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
            String sym = m_refs.getProperty( "Bundle-SymbolicName" );
            if (sym == null) {
                sym = "BuildByDirUrlHandler";
            }
            InputStream result = BndUtils.createBundle( fis, m_refs,sym );
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
