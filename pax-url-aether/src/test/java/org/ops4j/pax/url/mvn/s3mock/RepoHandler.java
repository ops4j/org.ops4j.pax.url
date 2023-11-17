/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.s3mock;

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
            "https", baseRequest.getScheme()  );

        Assert.assertEquals(
            "username and password should be injected as a header",
            "magic-token", request.getHeader( "User-Agent" ) );

        final String text = "hello there";

        response.getWriter().println( text );

        response.setContentType( "text/html" );

        response.setStatus( HttpServletResponse.SC_OK );

        baseRequest.setHandled( true );

    }

}
