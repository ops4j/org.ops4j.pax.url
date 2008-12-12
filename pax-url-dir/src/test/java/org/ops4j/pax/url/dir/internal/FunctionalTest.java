package org.ops4j.pax.url.dir.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import static org.junit.Assert.*;
import org.junit.Test;
import org.ops4j.io.StreamUtils;
import org.ops4j.pax.url.dir.bundle.BundleBuilder;
import org.ops4j.pax.url.dir.bundle.IntelliResourceLocator;
import org.ops4j.pax.url.dir.ResourceLocator;

/**
 * @author Toni Menzel (tonit)
 * @since Dec 11, 2008
 */
public class FunctionalTest
{

    @Test
    public void simpleRunWithRawApi()
        throws IOException
    {
        // construct a locator
        String clazz = this.getClass().getName().replaceAll( "\\.", "/" ) + ".class";
        ResourceLocator loc = new IntelliResourceLocator( new File( "." ), clazz );

        // construct the builder
        Properties p = new Properties();
        p.put( "Dynamic-Import", "*" );
        BundleBuilder b = new BundleBuilder( p, loc );

        // execute
        InputStream in = b.build();
        assertNotNull( in );

        // stream is filled lazily, so a complete read is important to verify
        dumpToConsole( in, 14 );
    }

    public static void dumpToConsole( InputStream in, int expecedEntries )
        throws IOException
    {
        JarInputStream jin = new JarInputStream( in );
        String[] s = readToc( jin );
        assertEquals( expecedEntries, s.length );
        Manifest man = jin.getManifest();
        assertNotNull( man );
        Attributes attributes = man.getMainAttributes();
        for( Object key : attributes.keySet() )
        {
            String v = attributes.getValue( (Attributes.Name) key );
            assertNotNull( key );
            assertNotNull( v );
            System.out.println( key + "=" + v );
        }
    }

    public static String[] readToc( JarInputStream jin )
        throws IOException
    {
        ArrayList<String> list = new ArrayList<String>();
        JarEntry entry = null;
        while( ( entry = jin.getNextJarEntry() ) != null )
        {
            System.out.println( entry.getName() );
            list.add( entry.getName() );
        }
        return list.toArray( new String[list.size()] );
    }

    public static void dumpToFile( InputStream in )
        throws IOException
    {
        FileOutputStream fos = new FileOutputStream( new File( "testout.jar" ) );
        StreamUtils.copyStream( in, fos, true );
    }

}
