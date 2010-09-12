/*
 * Copyright (C) 2010 Okidokiteam
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
package org.ops4j.pax.url.aether;

import org.junit.Test;
import org.ops4j.pax.url.aether.internal.AetherBasedResolver;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;

import java.io.File;
import java.io.IOException;

/**
 * Simply playing with aether api.
 */
public class AetherTest {
    @Test
    public void testMe() throws DependencyCollectionException, ArtifactResolutionException, IOException {


        String[] repos = "http://repo1.maven.org/maven2/".split(",");

        File localRepo = new File(new File(System.getProperty("user.home"),".m2"),"repository");

        String local = localRepo.getAbsolutePath();
        
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver(local,repos);

        aetherBasedResolver.resolve("org.apache.maven", "maven-profile", "jar", "2.2.1").close();

        aetherBasedResolver.resolve("org.apache.felix", "org.apache.felix.framework", "jar", "3.0.2" ).close();
        aetherBasedResolver.resolve("org.ops4j.pax.exam", "pax-exam-spi", "jar", "2.0.0-SNAPSHOT" ).close();


    }


}
