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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

public class SettingsTest {

    @Rule
    public TemporaryFolder fileRule = new TemporaryFolder();

    @Test
    public void settings_xml_in_path_that_contains_spaces() throws Exception {
        File folderWithSpaces = fileRule.newFolder("path with spaces");
        File settingsFile = new File(folderWithSpaces, "settings.xml");
        FileUtils.copyURLToFile(getClass().getResource("/settings/settingsWithLocalRepository.xml"), settingsFile);

        // simulate safeGetFile private method where spaces are replaced with %20
        String settingsFileURL = settingsFile.toURI().toURL().toExternalForm();
        assumeThat(settingsFileURL, containsString("%20"));

        Properties props = new Properties();
        props.put(ServiceConstants.PID + '.' + ServiceConstants.PROPERTY_SETTINGS_FILE, settingsFileURL);

        PropertiesPropertyResolver propertyResolver = new PropertiesPropertyResolver(props);
        MavenConfigurationImpl config = new MavenConfigurationImpl(propertyResolver, ServiceConstants.PID);

        Settings settings = config.getSettings();
        assertThat("settings.xml was not parsed because its path contains spaces",
                "repository", equalTo(settings.getLocalRepository()));
    }

}
