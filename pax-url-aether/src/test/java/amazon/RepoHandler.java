package amazon;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RepoHandler extends AbstractHandler
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    public void handle( final String target, final Request baseRequest,
            final HttpServletRequest request, final HttpServletResponse response )
        throws IOException, ServletException
    {

        log.info( "request : " + baseRequest );

        Assert.assertEquals( "this should work in https only",
            baseRequest.getScheme(), "https" );

        Assert.assertEquals(
            "username and password should be injected as a header",
            request.getHeader( "User-Agent" ), "magic-token" );

        final String text = "hello there";

        response.getWriter().println( text );

        response.setContentType( "text/html" );

        response.setStatus( HttpServletResponse.SC_OK );

        baseRequest.setHandled( true );

    }

}
