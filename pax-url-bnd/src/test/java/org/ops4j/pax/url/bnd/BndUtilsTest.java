package org.ops4j.pax.url.bnd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Toni Menzel (tonit)
 * @since Jan 13, 2009
 */
public class BndUtilsTest
{

    @Test
    public void emptyInstructionTest()
        throws MalformedURLException

    {
        Properties p = BndUtils.parseInstructions( "" );
        assertEquals( 0, p.size() );

    }

    @Test
    public void oneInstructionTest()
        throws MalformedURLException

    {
        Properties p = BndUtils.parseInstructions( "foo=bar" );
        assertEquals( "bar", p.getProperty( "foo" ) );
    }

    @Test
    public void multipleSimpleInstructionTest()
        throws MalformedURLException

    {
        Properties p = BndUtils.parseInstructions( "foo=bar&sing=sang&cheese=bacon" );
        assertEquals( "bar", p.getProperty( "foo" ) );
        assertEquals( "sang", p.getProperty( "sing" ) );
        assertEquals( "bacon", p.getProperty( "cheese" ) );

    }

    @Test
    public void complexOneInstructionTest()
        throws MalformedURLException

    {
        Properties p = BndUtils.parseInstructions( "Export-Package=*;version=\"2.4.0\"" );
        assertEquals( "*;version=\"2.4.0\"", p.getProperty( "Export-Package" ) );
    }

    @Test
    public void complexManyInstructionTest()
        throws MalformedURLException

    {
        Properties p = BndUtils.parseInstructions( "Export-Package=*;version=\"2.4.0\"&sec=two" );
        assertEquals( "*;version=\"2.4.0\"", p.getProperty( "Export-Package" ) );
        assertEquals( "two", p.getProperty( "sec" ) );
    }

}
