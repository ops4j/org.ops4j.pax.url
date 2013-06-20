/*
 * Copyright (C) 2010 Toni Menzel
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
package org.ops4j.pax.url.mvn;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Test;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenConstants;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simply playing with mvn api.
 */
public class AetherTest {

    private static Logger LOG = LoggerFactory.getLogger( AetherTest.class );

    @Test
    public void resolveArtifact()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getDummyConfig() );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "0.7.2" ).close();
    }

    @Test
    public void resolveRangeBased()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getDummyConfig() );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "LATEST" ).close();
    }

    @Test
    public void testCachingOfRanges()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {

        MavenConfiguration config = getDummyConfig();

        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( config );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "LATEST" ).close();

        // now again:
        // no repo
        aetherBasedResolver = new AetherBasedResolver( config );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "LATEST" ).close();
    }

    private MavenConfiguration getDummyConfig()
        throws IOException
    {
        Properties p = new Properties();
        p.setProperty( ServiceConstants.PID + MavenConstants.PROPERTY_LOCAL_REPOSITORY, getCache().toURI().toASCIIString() );
        p.setProperty( ServiceConstants.PID + MavenConstants.PROPERTY_REPOSITORIES,
                       "http://repo1.maven.org/maven2/@id=central,"
        );
        return new MavenConfigurationImpl( new PropertiesPropertyResolver( p ), ServiceConstants.PID );
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

    @Test
    public void testResolveRDF()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        Properties p = new Properties();
        p.setProperty( ServiceConstants.PID + MavenConstants.PROPERTY_LOCAL_REPOSITORY, getCache().toURI().toASCIIString() );
        p.setProperty( ServiceConstants.PID + MavenConstants.PROPERTY_REPOSITORIES,
                       "http://repository.jboss.org/nexus/content/repositories/thirdparty-releases@id=jboss"

        );
        MavenConfigurationImpl conf = new MavenConfigurationImpl( new PropertiesPropertyResolver( p ), ServiceConstants.PID );

        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( conf );
        aetherBasedResolver.resolve( "org.openrdf", "openrdf-model", "", "jar", "2.0.1" ).close();

    }
}

