package org.ops4j.pax.url.dir.internal.bundle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * This is a more convenient JarOutputStream that imits any writing of duplicate entries.
 * If someone tries to do so, it just does not write anything.
 * Be aware that this could be a source of trouble - but it simplifies merging jars with first come first serve policy.
 * <p/>
 * Implementation note: some silly class init starvation prevents constructor initialization of m_entrynames..
 *
 * @author Toni Menzel (tonit)
 * @since May 29, 2008
 */
public class DuplicateAwareJarOutputStream extends JarOutputStream
{

    private Set<String> m_entrynames;
    private boolean m_writable = false;

    public DuplicateAwareJarOutputStream( OutputStream outputStream, Manifest manifest )
        throws IOException
    {
        super( outputStream, manifest );
    }

    public DuplicateAwareJarOutputStream( OutputStream outputStream )
        throws IOException
    {
        super( outputStream );
    }

    public void write( int i )
        throws IOException
    {
        if( m_writable )
        {
            super.write( i );
        }
    }

    public synchronized void write( byte[] bytes, int i, int i1 )
        throws IOException
    {
        if( m_writable )
        {
            super.write( bytes, i, i1 );
        }
    }

    public void write( byte[] bytes )
        throws IOException
    {
        if( m_writable )
        {
            super.write( bytes );
        }
    }

    public void putNextEntry( ZipEntry zipEntry )
        throws IOException
    {
        if( m_entrynames == null )
        {
            m_entrynames = new HashSet<String>();
        }

        if( m_entrynames.add( zipEntry.getName() ) )
        {
            super.putNextEntry( zipEntry );
            m_writable = true;
        }
        else
        {
            m_writable = false;
        }
    }
}