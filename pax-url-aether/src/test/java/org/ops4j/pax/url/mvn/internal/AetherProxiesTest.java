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
package org.ops4j.pax.url.mvn.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

import static org.junit.Assert.assertFalse;

/**
 * Test cases for consecutive resolution attempts
 */
public class AetherProxiesTest {

    private static Server server;
    private static int port;

    private static ExecutorService pool = Executors.newFixedThreadPool(1);

    private static AtomicBoolean hitProxy = new AtomicBoolean(false);

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                try {
                    hitProxy.set(request.getHeader("Proxy-Connection") != null);
                    response.getOutputStream().write(new byte[] { 0x42 });
                    response.setStatus(HttpServletResponse.SC_OK);
                } finally {
                    baseRequest.setHandled(true);
                }
            }
        });
        server.start();
        port = server.getConnectors()[0].getLocalPort();
    }

    @Test
    public void activeProxy() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = mavenConfiguration();
        mavenConfiguration.setSettings(settingsWithDisabledProxy());
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        try {
            resolver.resolve("org.ops4j.pax.web",
                    "pax-web-api", "", "jar", "1");
        } finally {
            resolver.close();
        }
        assertFalse(hitProxy.get());
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
        pool.shutdown();
    }

    private MavenConfigurationImpl mavenConfiguration() {
        Properties properties = new Properties();
        properties.setProperty("pid.localRepository", "target/" + UUID.randomUUID().toString());
        properties.setProperty("pid.globalChecksumPolicy", "ignore");
        properties.setProperty("pid.connection.retryCount", "0");
        return new MavenConfigurationImpl(new PropertiesPropertyResolver(properties), "pid");
    }

    private Settings settingsWithDisabledProxy()
    {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();
        proxy.setActive(false);
        proxy.setId("proxy");
        proxy.setHost("localhost");
        proxy.setPort(port);
        proxy.setProtocol("http");
        settings.setProxies(Collections.singletonList(proxy));

        Profile defaultProfile = new Profile();
        defaultProfile.setId("default");
        Repository repo = new Repository();
        repo.setId("repo");
        repo.setUrl("http://localhost:" + port + "/repository");
        defaultProfile.addRepository(repo);

        settings.addProfile(defaultProfile);
        settings.addActiveProfile("default");
        return settings;
    }

}
