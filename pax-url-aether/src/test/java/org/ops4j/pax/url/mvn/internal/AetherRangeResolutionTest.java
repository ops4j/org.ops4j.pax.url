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
 */
package org.ops4j.pax.url.mvn.internal;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Similar to {@link AetherMultiTest} but without power mock
 */
public class AetherRangeResolutionTest {

    /**
     * KARAF-6005 - range resolution doesn't work inside ${karaf.home}/system
     */
    @Test
    public void noMetadataRangeResolutionWithDefaultRepository() throws IOException {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS, "repo2");
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        try {
            resolver.resolve("mvn:ant/ant/1.5.2");
        } catch (IOException e) {
            assertTrue(e.getCause() instanceof ArtifactResolutionException);
        }
        assertTrue(resolver.resolve("mvn:ant/ant/1.5.1").getCanonicalPath().endsWith("repo2/ant/ant/1.5.1/ant-1.5.1.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/[1.5,1.6)").getCanonicalPath().endsWith("repo2/ant/ant/1.5.1/ant-1.5.1.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/[1.5.1,1.5.2)").getCanonicalPath().endsWith("repo2/ant/ant/1.5.1/ant-1.5.1.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/LATEST").getCanonicalPath().endsWith("repo2/ant/ant/1.5.1/ant-1.5.1.jar"));
        try {
            resolver.resolve("mvn:ant/ant/[1.5.2,1.5.3)");
        } catch (IOException e) {
            assertTrue(e.getCause() instanceof VersionRangeResolutionException);
        }
    }

    /**
     * KARAF-6005 - range resolution doesn't work inside ${karaf.home}/system
     */
    @Test
    public void metadataRangeResolutionWithDefaultRepository() throws IOException {
        final MavenConfigurationImpl mavenConfiguration = basicMavenConfiguration(RepositoryPolicy.UPDATE_POLICY_ALWAYS, "repo3");
        AetherBasedResolver resolver = new AetherBasedResolver(mavenConfiguration);

        try {
            resolver.resolve("mvn:ant/ant/1.5.2");
        } catch (IOException e) {
            assertTrue(e.getCause() instanceof ArtifactResolutionException);
        }
        assertTrue(resolver.resolve("mvn:ant/ant/1.5.1").getCanonicalPath().endsWith("repo3/ant/ant/1.5.1/ant-1.5.1.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/[1.5,1.6)").getCanonicalPath().endsWith("repo3/ant/ant/1.5.1/ant-1.5.1.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/[1.5.1,1.5.2)").getCanonicalPath().endsWith("repo3/ant/ant/1.5.1/ant-1.5.1.jar"));
        assertTrue(resolver.resolve("mvn:ant/ant/LATEST").getCanonicalPath().endsWith("repo3/ant/ant/1.5.1/ant-1.5.1.jar"));
        try {
            resolver.resolve("mvn:ant/ant/[1.5.2,1.5.3)");
        } catch (IOException e) {
            assertTrue(e.getCause() instanceof VersionRangeResolutionException);
        }
    }

    private MavenConfigurationImpl basicMavenConfiguration(String globalUpdatePolicy, String repo) {
        Properties properties = new Properties();
        properties.setProperty("pid.localRepository", "target/" + UUID.randomUUID().toString());
        properties.setProperty("pid.repositories", "");

        File r = new File("src/test/resources", repo); // there should be ant:ant:1.5.1
        properties.setProperty("pid.defaultRepositories", r.toURI().toString() + "@id=my-" + repo);
        properties.setProperty("pid.globalChecksumPolicy", "ignore");
        properties.setProperty("pid.globalUpdatePolicy", globalUpdatePolicy);
        properties.setProperty("pid.connection.retryCount", "0");
        MavenConfigurationImpl mavenConfiguration = new MavenConfigurationImpl(new PropertiesPropertyResolver(properties), "pid");
        return mavenConfiguration;
    }

}
