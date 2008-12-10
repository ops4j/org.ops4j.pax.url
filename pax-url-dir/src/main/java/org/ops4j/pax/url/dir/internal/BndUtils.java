package org.ops4j.pax.url.dir.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import org.ops4j.lang.NullArgumentException;

/**
 * BND related utilities.
 * Derived from pax url bnd.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @author Toni Menzel (tonit)
 * @since May 29, 2008
 *
 */
public class BndUtils
{

    /**
     * Regex pattern for matching instructions when specified in url.
     */
    private static final Pattern INSTRUCTIONS_PATTERN =
        Pattern.compile( "([a-zA-Z_0-9-]+)=([-!\"'()*+,.0-9A-Z_a-z%]+)" );

    /**
     * Utility class. Ment to be used using static methods
     */
    private BndUtils()
    {
        // utility class
    }

    /**
     * Precesses the input jar and generates the necessary OSGi headers using specified instructions.
     *
     * @param jarInputStream input stream for the jar to be processed. Cannot be null.
     * @param instructions   bnd specific processing instructions. Cannot be null.
     * @param jarInfo        information about the jar to be processed. Usually the jar url. Cannot be null or empty.
     *
     * @return an input strim for the generated bundle
     *
     * @throws org.ops4j.lang.NullArgumentException
     *                             if any of the paramters is null
     * @throws java.io.IOException re-thron during jar processing
     */
    public static InputStream createBundle( final InputStream jarInputStream,
                                            final Properties instructions,
                                            final String jarInfo )
        throws IOException
    {
        NullArgumentException.validateNotNull( jarInputStream, "Jar URL" );
        NullArgumentException.validateNotNull( instructions, "Instructions" );
        NullArgumentException.validateNotEmpty( jarInfo, "Jar info" );

        final Jar jar = new Jar( "dot", jarInputStream );
        final Manifest manifest = jar.getManifest();

        final Properties properties = new Properties( instructions );
        properties.setProperty( "Generated-By-Ops4j-Pax-From", jarInfo );
        properties.setProperty( "DynamicImport-Package", "*" );
        final Analyzer analyzer = new Analyzer();
        analyzer.setJar( jar );
        analyzer.setProperties( properties );
        checkMandatoryProperties( analyzer, jar, jarInfo );
        analyzer.mergeManifest( manifest );
        analyzer.calcManifest();
        return createInputStream( jar );
    }

    /**
     * Creates an piped input stream for the wrapped jar.
     * This is done in a thread so we can return quickly.
     *
     * @param jar the wrapped jar
     *
     * @return an input stream for the wrapped jar
     *
     * @throws java.io.IOException re-thrown
     */
    private static PipedInputStream createInputStream( final Jar jar )
        throws IOException
    {
        final PipedInputStream pin = new PipedInputStream();
        final PipedOutputStream pout = new PipedOutputStream( pin );

        new Thread()
        {
            public void run()
            {
                try
                {
                    jar.write( pout );
                }
                catch( IOException e )
                {
                    throw new RuntimeException( "Bundle cannot be generated", e );
                }
                finally
                {
                    try
                    {
                        jar.close();
                        pout.close();
                    }
                    catch( IOException ignore )
                    {
                        // if we getProperty here something is very wrong
                        throw new RuntimeException( "Cannot close ???", ignore );
                    }
                }
            }
        }.start();

        return pin;
    }

    /**
     * Check if manadatory properties are present, otherwise generate default.
     *
     * @param analyzer     bnd analyzer
     * @param jar          bnd jar
     * @param symbolicName bundle symbolic name
     */
    private static void checkMandatoryProperties( final Analyzer analyzer,
                                                  final Jar jar,
                                                  final String symbolicName )
    {
        final String importPackage = analyzer.getProperty( Analyzer.IMPORT_PACKAGE );
        if( importPackage == null || importPackage.trim().length() == 0 )
        {
            analyzer.setProperty( Analyzer.IMPORT_PACKAGE, "*;resolution:=optional" );
        }
        final String exportPackage = analyzer.getProperty( Analyzer.EXPORT_PACKAGE );
        if( exportPackage == null || exportPackage.trim().length() == 0 )
        {
            analyzer.setProperty( Analyzer.EXPORT_PACKAGE, analyzer.calculateExportsFromContents( jar ) );
        }
        final String localSymbolicName = analyzer.getProperty( Analyzer.BUNDLE_SYMBOLICNAME, symbolicName );
        analyzer.setProperty( Analyzer.BUNDLE_SYMBOLICNAME, generateSymbolicName( localSymbolicName ) );
    }

    /**
     * Processes symbolic name and replaces osgi spec invalid characters with "_".
     *
     * @param symbolicName bundle symbolic name
     *
     * @return a valid symbolic name
     */
    private static String generateSymbolicName( final String symbolicName )
    {
        return symbolicName.replaceAll( "[^a-zA-Z_0-9.-]", "_" );
    }

}
