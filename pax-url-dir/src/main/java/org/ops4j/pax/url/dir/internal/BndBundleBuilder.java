package org.ops4j.pax.url.dir.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import org.ops4j.lang.NullArgumentException;

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
    private Properties m_ref;

    /**
     * This is where additional info from builing this can be place and used lated from inside osgi.
     * Will be put into manifest of this bundle.
     */
    private static final String REFERENCE_INFORMATION = "PAX-URL-DIR-REFERENCE";
    private Properties m_refs;

    /**
     * Constructor.
     *
     * @param ref        name of test class
     * @param testMethod name of the test method
     * @param finder     locator that gathers all resources that have to be inside the test probe
     */
    public BndBundleBuilder( final Properties ref,
                             final ResourceLocator finder )
    {
        NullArgumentException.validateNotNull( ref, "recipeHost" );
        NullArgumentException.validateNotNull( finder, "finder" );

        m_finder = finder;
        m_ref = ref;

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

            Properties props = new Properties();
            props.putAll( m_refs );

            // TODO set args on BndUtils
            InputStream result = BndUtils.createBundle( fis, props, "TOBESET" );
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
