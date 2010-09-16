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
public class AetherTest
{

    @Test
    public void resolveArtifact()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        String[] repos = "http://repo1.maven.org/maven2/,http://scm.ops4j.org/repos/ops4j/projects/pax/runner-repository/,".split( "," );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( "target/local1", repos );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "jar", "0.7.2" ).close();

    }

    @Test
    public void resolveRangeBased()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        String[] repos = "http://repo1.maven.org/maven2/,http://scm.ops4j.org/repos/ops4j/projects/pax/runner-repository/,".split( "," );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( "target/local2", repos );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "jar", "(0.0,]" ).close();
    }
}

