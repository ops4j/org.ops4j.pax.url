/*
 * Copyright (C) 2010 Toni Menzel
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
package org.ops4j.pax.url.mvn.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Tries to resolve with Aether if installed. Othwise just uses the "old" one.
 * Small performance penalty of Aeather URL Handler is not installed. Should be settable by property though.
 */
public class AetherBridgeConnection extends URLConnection {

    private URLConnection m_fallback;

    private static final Log LOG = LogFactory.getLog(AetherBridgeConnection.class);

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url      the specified URL.
     * @param fallback Fallback connection if aether is not available.
     */
    public AetherBridgeConnection(URL url, URLConnection fallback) {
        super(url);
        m_fallback = fallback;
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream()
            throws IOException {

        try {

            return new URL( url.toExternalForm().replaceFirst("mvn", "aether")).openStream();
        } catch (MalformedURLException e) {
            LOG.debug("Ather URL Handler not available. Using mvn fallback to resolve " + url.toExternalForm());
        } catch (Exception e) {
            // other exceptions. don't try to resolve otherwise:
            throw new IOException(e);
        }

        return m_fallback.getInputStream();
    }
}
