package org.ops4j.pax.url.mvnlive.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.runner.platform.PlatformException;
import org.ops4j.pax.runner.platform.internal.Pipe;

/**
 * @author Toni Menzel (tonit)
 * @since Jul 10, 2008
 */
public class Connection extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Connection.class );

    private Configuration m_configuration;
    private Parser m_parser;
    private static final int MAVEN_TIMEOUT = 60000;

    public Connection( URL url, Configuration config )
    {
        super( url );
        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( config, "Service configuration" );
        m_configuration = config;
        m_parser = new Parser( url.getPath() );

    }

    public void connect()
        throws IOException
    {
        // do nothing.
    }

    public InputStream getInputStream()
        throws IOException
    {
        connect();

        LOG.debug( "Resolving [" + url.toExternalForm() + "]" );
        File f = m_parser.getProjectFolder();
        if( hasChanges( f ) )
        {
            triggerBuild( f );
        }
        return new FileInputStream( getOutputArtifact( f ) );
    }

    /**
     * Will locate the built artifact under projectRoot/target and return. If invalid: null
     *
     * @param projectRoot the root of the artifact you are currently working on.
     */
    private File getOutputArtifact( File projectRoot )
    {
        try
        {
            for( File sub : new File( projectRoot.getCanonicalPath() + "/target" ).listFiles() )
            {
                // TODO there can be more than one artifact in target (like java 1.4 retrobuilds)
                if( sub.getName().endsWith( ".jar" ) )
                {
                    return sub;
                }
            }
        } catch( IOException ioE )
        {
            throw new RuntimeException( ioE );
        }
        // nothing found. Sad enough.
        return null;
    }

    /**
     * Will trigger a "mvn clean install"
     *
     * @param projectRoot
     */
    private void triggerBuild( File projectRoot )
    {
        try
        {
            LOG.info( "Root is " + projectRoot.getCanonicalPath() );
            String[] commandLine = new String[]{
                getMavenExecutable(),
                "clean",
                "install"
            };
            Process frameworkProcess = Runtime.getRuntime().exec( commandLine, null, projectRoot );

            Thread shutdownHook = createShutdownHook( frameworkProcess );
            Runtime.getRuntime().addShutdownHook( shutdownHook );
            long start = System.currentTimeMillis();
            int ex = -1;
            do
            {
                try
                {
                    ex = frameworkProcess.exitValue();
                    LOG.info( "maven returned with value: " + ex );
                    break;
                } catch( IllegalThreadStateException e )
                {
                    // ignore the fact that the process is still running..
                }
            } while( start + MAVEN_TIMEOUT > System.currentTimeMillis() );

        } catch( Exception e )
        {
            throw new RuntimeException( e );
        }

    }

    private String getMavenExecutable()
        throws PlatformException
    {
        String home = System.getProperty( "M2_HOME" );
        if( home == null )
        {
            home = System.getenv( "M2_HOME" );
        }

        if( home == null )
        {
            throw new PlatformException( "M2_HOME is not set." );
        }
        return home + "/bin/mvn";
    }

    /**
     * Create helper thread to safely shutdown the external framework process
     *
     * @param process framework process
     *
     * @return stream handler
     */
    private Thread createShutdownHook( final Process process )
    {
        final Pipe errPipe = new Pipe( process.getErrorStream(), System.err ).start( "Error pipe" );
        final Pipe outPipe = new Pipe( process.getInputStream(), System.out ).start( "Out pipe" );
        final Pipe inPipe = new Pipe( process.getOutputStream(), System.in ).start( "In pipe" );

        Thread shutdownHook = new Thread( new Runnable()
        {
            public void run()
            {
                inPipe.stop();
                outPipe.stop();
                errPipe.stop();

                try
                {
                    process.destroy();
                }
                catch( Exception e )
                {
                    // ignore if already shutting down
                }
            }
        }, "mvnlive url handler shutdown hook"
        );

        return shutdownHook;
    }

    private boolean hasChanges( File f )
    {
        // TODO add folder sensor from okidoki here

        return true;
    }
}
