package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.io.StreamUtils;

/**
 * Finds resources of the current module under test just by given top-level parent (whatever that is)
 * and name of the class under test using a narrowing approach.
 *
 * @author Toni Menzel (tonit)
 * @since May 30, 2008
 */
public class IntelliResourceLocator implements ResourceLocator
{

    public static final Log logger = LogFactory.getLog( IntelliResourceLocator.class );

    private File m_topLevelDir;
    private String m_targetClassName;

    public IntelliResourceLocator( File topLevelDir, String targetClassName )
    {
        NullArgumentException.validateNotNull( topLevelDir, "topLevelDir" );
        NullArgumentException.validateNotEmpty( targetClassName, "targetClassName" );
        if( !topLevelDir.exists() || !topLevelDir.canRead() || !topLevelDir.isDirectory() )
        {
            throw new IllegalArgumentException(
                "topLevelDir " + topLevelDir.getAbsolutePath() + " is not a readable folder"
            );
        }
        m_topLevelDir = topLevelDir;
        m_targetClassName = targetClassName;
    }

    public IntelliResourceLocator( String targetClassName )
    {
        this( new File( "." ), targetClassName );
    }

    /**
     * This locates the top level resource folders for the current component
     *
     * @param target to write to
     */
    public void write( JarOutputStream target )
        throws IOException
    {

        // determine the real base
        m_targetClassName = m_targetClassName.replace( '.', File.separatorChar ) + ".class";
        File root = findRoot( m_topLevelDir, m_targetClassName );
        findClasspathResources( target, root, root );
        if( root != null )
        {
            // convention: use mvn naming conventions to locate the other resource folders
            File pureClasses = new File( root.getParentFile().getCanonicalPath() + "/classes/" );
            findClasspathResources( target, pureClasses, pureClasses );
        }
        else
        {
            throw new IllegalArgumentException( "Class " + m_targetClassName + " has not been found!" );
        }


    }

    protected File findRoot( File dir, String targetClassName )
        throws IOException
    {
        for( File f : dir.listFiles() )
        {
            if( !f.isHidden() && f.isDirectory() )
            {
                File r = findRoot( f, targetClassName );
                if( r != null )
                {
                    return r;
                }
            }
            else if( !f.isHidden() && f.getCanonicalPath().endsWith( targetClassName ) )
            {
                return new File(
                    f.getCanonicalPath().substring( 0, f.getCanonicalPath().length() - targetClassName.length() )
                );
            }
        }
        // nothing found / must be wrong topevel dir
        return null;
    }

    private void findClasspathResources( JarOutputStream target, File dir, File base )
        throws IOException
    {
        if( dir != null && dir.canRead() && dir.isDirectory() )
        {
            for( File f : dir.listFiles() )
            {
                if( f.isDirectory() )
                {
                    findClasspathResources( target, f, base );
                }
                else if( !f.isHidden() )
                {
                    writeToTarget( target, f, base );
                    //repo.add(f);
                }
            }
        }
    }

    private void writeToTarget( JarOutputStream target, File f, File base )
        throws IOException
    {
        String name =
            f.getCanonicalPath().substring( base.getCanonicalPath().length() + 1 ).replace( File.separatorChar, '/' );
        if( name.equals( "META-INF/MANIFEST.MF" ) )
        {
            throw new RuntimeException( "You have specified a " + name
                                        + " in your probe bundle. Please make sure that you don't have it in your project's target folder. Otherwise it would lead to false assumptions and unexpected results."
            );
        }
        target.putNextEntry( new JarEntry( name ) );
        FileInputStream fis = new FileInputStream( f );
        try
        {
            StreamUtils.copyStream( fis, target, false );
        } finally
        {
            fis.close();
        }
    }
}
