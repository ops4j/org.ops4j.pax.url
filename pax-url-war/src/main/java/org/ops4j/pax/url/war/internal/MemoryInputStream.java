package org.ops4j.pax.url.war.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: alindreghiciu
 * Date: Jan 14, 2008
 * Time: 3:12:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class MemoryInputStream
    extends InputStream
{

    private final InputStream m_wrapped;
    private final MemoryRepository.Reference m_reference;

    MemoryInputStream( final InputStream wrapped, MemoryRepository.Reference reference )
    {
        m_wrapped = wrapped;
        m_reference = reference;
    }

    public int available()
        throws IOException
    {
        return m_wrapped.available();
    }

    public void close()
        throws IOException
    {
        try
        {
            m_wrapped.close();
        }
        finally
        {
            m_reference.remove();
        }
    }

    public void mark( int i )
    {
        m_wrapped.mark( i );
    }

    public boolean markSupported()
    {
        return m_wrapped.markSupported();
    }

    public int read()
        throws IOException
    {
        return m_wrapped.read();
    }

    public int read( byte[] bytes )
        throws IOException
    {
        return m_wrapped.read( bytes );
    }

    public int read( byte[] bytes, int i, int i1 )
        throws IOException
    {
        return m_wrapped.read( bytes, i, i1 );
    }

    public void reset()
        throws IOException
    {
        m_wrapped.reset();
    }

    public long skip( long l )
        throws IOException
    {
        return m_wrapped.skip( l );
    }
}
