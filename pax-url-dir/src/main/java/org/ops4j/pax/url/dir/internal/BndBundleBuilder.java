package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.io.StreamUtils;

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
    private String m_testClass;
    private String m_testMethod;

    /**
     * This is where additional info from builing this can be place and used lated from inside osgi.
     * Will be put into manifest of this bundle.
     */
    private static final String REFERENCE_INFORMATION = "PAX-URL-DIR-REFERENCE";

    /**
     * Constructor.
     *
     * @param testClass  name of test class
     * @param testMethod name of the test method
     * @param finder     locator that gathers all resources that have to be inside the test probe
     */
    public BndBundleBuilder( final String testClass,
                                final String testMethod,
                                final ResourceLocator finder )
    {
        NullArgumentException.validateNotNull( testClass, "recipeHost" );
        NullArgumentException.validateNotNull( finder, "finder" );

        m_finder = finder;
        m_testClass = testClass;
        m_testMethod = testMethod;
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

            // 2. wrap and calc manifest using bnd
            Properties props = new Properties();
            // Recipe Host
            String info = m_testClass + ":" + m_testMethod;
            props.put( REFERENCE_INFORMATION, info );

            // include connector clazzes to be used inside
            //props.put( "Private-Package", "org.ops4j.pax.exam.connector.*" );
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

    /**
     * Store result into an intermediate file. Just used for now to see what actually is inside the stream.
     *
     * @param inputStream stream will be store into a local file
     *
     * @return a new stream reading from that file.
     */
    private InputStream traceResultBundle( InputStream inputStream )
        throws IOException
    {
        File finalOutput = File.createTempFile( "paxExamFinal", "jar" );
        FileOutputStream outstream = new FileOutputStream( finalOutput );
        StreamUtils.copyStream( inputStream, outstream, true );
        return new FileInputStream( finalOutput );
    }


}
