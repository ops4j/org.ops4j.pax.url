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
package org.ops4j.pax.url.mvn;

import java.io.IOException;
import java.io.StringWriter;
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
public class AetherSnapshotUpdatesTest {

    private static Server server;
    private static int port;

    private static boolean someoneDeploysNewerSnapshot = false;
    private static Map<String, Integer> HITS = new HashMap<>();

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                try {
                    if (request.getRequestURI().endsWith("maven-metadata.xml")) {
                        String[] split = request.getRequestURI().split("/");
                        int version = Integer.parseInt(split[split.length - 2].split("-")[0]);

                        String ts1 = someoneDeploysNewerSnapshot ? "20161017111010" : "20161017101010";
                        String ts2 = someoneDeploysNewerSnapshot ? "20161017.111010" : "20161017.101010";
                        String nr = someoneDeploysNewerSnapshot ? "2" : "1";

                        response.setStatus(HttpServletResponse.SC_OK);
                        StringWriter sw = new StringWriter();
                        sw.append("<metadata>\n");
                        sw.append("  <groupId>org.ops4j.pax.web</groupId>\n");
                        sw.append("  <artifactId>pax-web-api</artifactId>\n");
                        sw.append("  <version>1-SNAPSHOT</version>\n");
                        sw.append("  <versioning>\n");
                        sw.append("    <snapshot>\n");
                        sw.append("      <timestamp>" + ts2 + "</timestamp>\n");
                        sw.append("      <buildNumber>" + nr + "</buildNumber>\n");
                        sw.append("    </snapshot>\n");
                        sw.append("    <lastUpdated>" + ts1 + "</lastUpdated>\n");
                        sw.append("    <snapshotVersions>\n");
                        sw.append("      <snapshotVersion>\n");
                        sw.append("        <extension>jar</extension>\n");
                        sw.append("        <value>1-" + ts2 + "-" + nr + "</value>\n");
                        sw.append("        <updated>" + ts1 + "</updated>\n");
                        sw.append("      </snapshotVersion>\n");
                        sw.append("    </snapshotVersions>\n");
                        sw.append("  </versioning>\n");
                        sw.append("</metadata>\n");

                        response.getOutputStream().write(sw.toString().getBytes("UTF-8"));
                    } else if (request.getRequestURI().endsWith(".jar")) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getOutputStream().write(0x42);
                    }
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
        someoneDeploysNewerSnapshot = false;
    }

    @Test
    public void updatePolicyAlways() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/maven-metadata.xml"),
                equalTo(3));
        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/pax-web-api-1-20161017.101010-1.jar"),
                equalTo(1));
    }

    @Test
    public void updatePolicyNever() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_NEVER);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/maven-metadata.xml"),
                equalTo(1));
        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/pax-web-api-1-20161017.101010-1.jar"),
                equalTo(1));
    }

    @Test
    public void updatePolicyAlwaysWithSnapshotUpdatedRemotely() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        someoneDeploysNewerSnapshot = true;
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");

        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/maven-metadata.xml"),
                equalTo(3));
        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/pax-web-api-1-20161017.101010-1.jar"),
                equalTo(1));
        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/pax-web-api-1-20161017.111010-2.jar"),
                equalTo(1));
    }

    @Test
    public void updatePolicyNeverWithSnapshotUpdatedRemotely() throws Exception {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_NEVER);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        someoneDeploysNewerSnapshot = true;
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");
        resolver.resolve("org.ops4j.pax.web", "pax-web-api",
                "", "jar", "1-SNAPSHOT");

        assertThat(HITS.size(), equalTo(2));
        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/maven-metadata.xml"),
                equalTo(1));
        assertThat(HITS.get("/repository/org/ops4j/pax/web/pax-web-api/1-SNAPSHOT/pax-web-api-1-20161017.101010-1.jar"),
                equalTo(1));
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
    }

    private MavenConfigurationImpl basicMavenConfiguration(String globalUpdatePolicy) {
        Properties properties = new Properties();
        properties.setProperty("pid.localRepository", "target/" + UUID.randomUUID().toString());
        properties.setProperty("pid.timeout", "1000");
        properties.setProperty("pid.repositories", "http://everfree-forest/repository@id=single-repo@snapshots");
        properties.setProperty("pid.globalChecksumPolicy", "ignore");
        properties.setProperty("pid.globalUpdatePolicy", globalUpdatePolicy);
        properties.setProperty("pid.connection.retryCount", "0");
        MavenConfigurationImpl mavenConfiguration = new MavenConfigurationImpl(new PropertiesPropertyResolver(properties), "pid");
        mavenConfiguration.setSettings(settingsWithProxy());
        return mavenConfiguration;
    }

    private Settings settingsWithProxy()
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
