/*
 * Copyright (C) 2012 Andrei Pozolotin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.s3mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Util
{

    static Logger LOG = LoggerFactory.getLogger( Util.class );

    /** server host in settings.xml */
    static String getHost()
    {
        return "localhost";
    }

    /** server port in settings.xml */
    static int getPort()
    {
        return 11443;
    }

    static File getTestKeystore() throws IOException
    {
        return new File( "./src/test/resources/amazon/keystore.jks" );
    }

    static String getTestKeystorePassword() throws IOException
    {
        return "wicket";
    }

    static File getTestSettings() throws IOException
    {
        return new File( "./src/test/resources/amazon/settings.xml" );
    }

    static File getTestRepo() throws IOException
    {

        File folder = new File( "./target" );
        folder.mkdir();

        File file = File.createTempFile( "test", ".repo", folder );
        file.delete();
        file.mkdirs();

        LOG.info( "test repo : " + file.getAbsolutePath() );

        return file;

    }

    static void setupClientSSL() throws Exception
    {
        KeyStore store = KeyStore.getInstance( KeyStore.getDefaultType() );

        FileInputStream storeInput = new FileInputStream( getTestKeystore() );

        char[] storePass = getTestKeystorePassword().toCharArray();

        store.load( storeInput, storePass );

        TrustManagerFactory manager = TrustManagerFactory
            .getInstance( TrustManagerFactory.getDefaultAlgorithm() );

        manager.init( store );

        SSLContext context = SSLContext.getInstance( "TLSv1.2" );

        context.init( null, manager.getTrustManagers(), null );

        SSLSocketFactory factory = context.getSocketFactory();

        HttpsURLConnection.setDefaultSSLSocketFactory( factory );

        HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier()
        {
            public boolean verify( //
                    String hostname, SSLSession session )
            {
                return true;
            }
        } );

    }

}
