/*
 * Copyright 2012 Andrei Pozolotin.
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

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * emulate AWS S3
 */
public abstract class TestBase
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private Server server;

    @Before
    public void startHttp() throws Exception
    {
        /* client setup */

        /* TODO should work w/o this ? */
        Util.setupClientSSL();

        /* server setup */

        server = new Server();

        final SslContextFactory.Server factory = new SslContextFactory.Server();
        factory.setKeyStorePath( Util.getTestKeystore().getAbsolutePath() );
        factory.setKeyStorePassword( Util.getTestKeystorePassword() );
        factory.setTrustStorePath( Util.getTestKeystore().getAbsolutePath() );
        factory.setKeyManagerPassword( Util.getTestKeystorePassword() );

        final ServerConnector connector = new ServerConnector( server,  factory );
        ((HttpConnectionFactory) connector.getConnectionFactory("HTTP/1.1")).getHttpConfiguration().getCustomizer(SecureRequestCustomizer.class).setSniHostCheck(false);
        connector.setPort( Util.getPort() );

        server.addConnector( connector );

        server.setHandler( new RepoHandler() );

        server.start();

        log.info( "init" );

    }

    @After
    public void stopHttp() throws Exception
    {

        server.stop();

        log.info( "done" );

    }

}
