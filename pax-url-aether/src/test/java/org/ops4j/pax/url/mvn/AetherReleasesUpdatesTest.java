/*
 *  Copyright 2016 Grzegorz Grzybek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ops4j.pax.url.mvn;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test cases for connection and read timeouts
 */
public class AetherReleasesUpdatesTest {

    private static Server server;
    private static int port;

    private static Map<String, Integer> HITS = new HashMap<>();

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                try {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(0x42);
                } finally {
                    if (!HITS.containsKey(request.getRequestURI())) {
                        HITS.put(request.getRequestURI(), 0);
                    }
                    HITS.put(request.getRequestURI(), HITS.get(request.getRequestURI()) + 1);
                    baseRequest.setHandled(true);
                }
            }
        });
        server.start();
        port = server.getConnectors()[0].getLocalPort();
    }

    @Before
    public void init() {
        HITS.clear();
    }

    @Test
    public void updatePolicyAlwaysNonCanonical() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS, true);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1/pax-web-api-1.jar"),
                equalTo(3));
    }

    @Test
    public void updatePolicyAlwaysCanonical() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS, false);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1/pax-web-api-1.jar"),
                equalTo(1));
    }

    @Test
    public void updatePolicyNeverNonCanonical() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_NEVER, true);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1/pax-web-api-1.jar"),
                equalTo(1));
    }

    @Test
    public void updatePolicyNeverCanonical() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_NEVER, false);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1/pax-web-api-1.jar"),
                equalTo(1));
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
    }

    private MavenConfigurationImpl basicMavenConfiguration(String globalUpdatePolicy, boolean updateReleases) {
        Properties properties = getProperties(globalUpdatePolicy, updateReleases);
        MavenConfigurationImpl mavenConfiguration = new MavenConfigurationImpl(new PropertiesPropertyResolver(properties), "pid");
        mavenConfiguration.setSettings(getSettings(globalUpdatePolicy));
        return mavenConfiguration;
    }

    protected Properties getProperties(String globalUpdatePolicy, boolean updateReleases) {
        Properties properties = new Properties();
        properties.setProperty("pid.localRepository", "target/" + UUID.randomUUID().toString());
        properties.setProperty("pid.timeout", "1000");
        properties.setProperty("pid.repositories", "http://everfree-forest/repository@id=single-repo@snapshots");
        properties.setProperty("pid.globalChecksumPolicy", "ignore");
        properties.setProperty("pid.globalUpdatePolicy", globalUpdatePolicy);
        properties.setProperty("pid.updateReleases", Boolean.toString(updateReleases));
        properties.setProperty("pid.connection.retryCount", "0");
        return properties;
    }

    protected Settings getSettings(String globalUpdatePolicy)
    {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();
        proxy.setId("proxy");
        proxy.setHost("localhost");
        proxy.setPort(port);
        proxy.setProtocol("http");
        settings.setProxies(Collections.singletonList(proxy));

        return settings;
    }

}
