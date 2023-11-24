/*
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.ops4j.pax.url.mvn.internal;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

import static org.junit.Assert.assertTrue;

/**
 * Similar to {@link AetherMultiTest} but without power mock
 */
public class AetherMultiResolutionTest {

    /**
     * PAXURL-314 - @snapshots does not work with @multi
     */
    @Test
    public void multiRepoWithSnapshots() throws IOException {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        assertTrue(resolver.resolve("mvn:ant/ant/1.5.1-SNAPSHOT").getCanonicalPath().endsWith("r1/ant/ant/1.5.1-SNAPSHOT/ant-1.5.1-SNAPSHOT.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/1.5.2-SNAPSHOT").getCanonicalPath().endsWith("r2/ant/ant/1.5.2-SNAPSHOT/ant-1.5.2-SNAPSHOT.jar"));
        resolver.close();
    }

    private MavenConfigurationImpl basicMavenConfiguration(String globalUpdatePolicy) {
        Properties properties = new Properties();
        properties.setProperty("pid.localRepository", "target/" + UUID.randomUUID().toString());
        properties.setProperty("pid.repositories", "");

        File multiRepoRoot = new File("src/test/resources", "repomulti_snapshots");
        properties.setProperty("pid.defaultRepositories", multiRepoRoot.toURI().toString() + "@multi@snapshots@id=my-multirepo");
        properties.setProperty("pid.globalChecksumPolicy", "ignore");
        properties.setProperty("pid.globalUpdatePolicy", globalUpdatePolicy);
        properties.setProperty("pid.connection.retryCount", "0");
        return new MavenConfigurationImpl(new PropertiesPropertyResolver(properties), "pid");
    }

}
