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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.ops4j.pax.url.aether.internal.AetherBasedResolver;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.util.property.PropertiesPropertyResolver;

/**
 * Simply playing with aether api.
 */
public class AetherTest {

    private static Logger LOG = LoggerFactory.getLogger( AetherTest.class );

    @Test
    public void resolveArtifact()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        String[] repos = "http://repo1.maven.org/maven2/,http://scm.ops4j.org/repos/ops4j/projects/pax/runner-repository/,".split( "," );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getDummyConfig(), Arrays.asList( repos ) );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "jar", "0.7.2" ).close();
    }

    @Test
    public void resolveRangeBased()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        String[] repos = "http://repo1.maven.org/maven2/,http://scm.ops4j.org/repos/ops4j/projects/pax/runner-repository/,".split( "," );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getDummyConfig(), Arrays.asList( repos ) );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "jar", "LATEST" ).close();
    }

    @Test
    public void resolveFakeRepo()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        String[] repos = "http://repo1.maven.org/maven2/,http://scm.ops4j.org/repos/ops4j/projects/pax/runner-repository/,".split( "," );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getDummyConfig(), Arrays.asList( repos ) );
        aetherBasedResolver.resolve( "org.ops4j.pax.runner.profiles", "ds", "composite", "LATEST" ).close();
    }

    @Test
    public void testCachingOfRanges()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        String[] repos = "http://repo1.maven.org/maven2/,http://scm.ops4j.org/repos/ops4j/projects/pax/runner-repository/,".split( "," );

        MavenConfiguration config = getDummyConfig();
        
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( config, Arrays.asList( repos ) );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "jar", "LATEST" ).close();

        // now again:
        // no repo
        aetherBasedResolver = new AetherBasedResolver( config, Arrays.asList( "" ) );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "jar", "LATEST" ).close();
    }

    private File getCache()
        throws IOException
    {
        File base = new File( "target" );
        base.mkdir();
        File f = File.createTempFile( "aethertest", ".dir", base );
        f.delete();
        f.mkdirs();
        LOG.info( "Caching" + " to " + f.getAbsolutePath() );
        return f;
    }

    private MavenConfiguration getDummyConfig()
        throws IOException
    {
        final MavenRepositoryURL localPath = new MavenRepositoryURL( getCache().toURI().toASCIIString() );
        return new MavenConfigurationImpl( new PropertiesPropertyResolver( System.getProperties() ), ServiceConstants.PID ) {
            @Override
            public MavenRepositoryURL getLocalRepository()
            {
                return localPath;
            }
        };
    }
}

