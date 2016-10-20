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

import java.util.Properties;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;

public class AetherReleasesUpdatesWithSettingsTest extends AetherReleasesUpdatesTest {

    @Override
    protected Properties getProperties(String globalUpdatePolicy, boolean updateReleases) {
        Properties properties = super.getProperties(globalUpdatePolicy, updateReleases);
        properties.remove("pid.globalUpdatePolicy");
        properties.setProperty("pid.repositories", "+");
        return properties;
    }

    @Override
    protected Settings getSettings(String globalUpdatePolicy) {
        Settings settings = super.getSettings(globalUpdatePolicy);
        settings.addActiveProfile("p1");
        Profile p1 = new Profile();
        p1.setId("p1");
        Repository r = new Repository();
        r.setId("single-repo");
        RepositoryPolicy releasesPolicy = new RepositoryPolicy();
        releasesPolicy.setChecksumPolicy("ignore");
        releasesPolicy.setEnabled(true);
        releasesPolicy.setUpdatePolicy(globalUpdatePolicy);
        r.setReleases(releasesPolicy);
        r.setUrl("http://everfree-forest/repository");
        p1.getRepositories().add(r);
        settings.addProfile(p1);
        return settings;
    }

}
