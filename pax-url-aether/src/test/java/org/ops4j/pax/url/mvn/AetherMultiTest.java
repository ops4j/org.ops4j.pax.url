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

import junit.framework.Assert;

import org.junit.Test;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;

/**
 * Simply playing with mvn api.
 */
public class AetherMultiTest {

    private static Logger LOG = LoggerFactory.getLogger( AetherMultiTest.class );

    @Test
    public void resolveArtifactUsingMulti()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getDummyConfig() );
        aetherBasedResolver.resolve( "ant", "ant", "", "jar", "1.5.1" ).close();
    }

    private MavenConfiguration getDummyConfig()
        throws IOException
    {
        Properties p = new Properties();
        p.setProperty( ServiceConstants.PID + MavenConstants.PROPERTY_LOCAL_REPOSITORY, getCache().toURI().toASCIIString() );
        
        File target = new File("target/test-classes/repomulti");
        Assert.assertTrue("Can not find test repo " + target.toURI().toString(), target.isDirectory());
        String multiRepo = target.toURI().toString() + "@id=multitest@multi";
        p.setProperty( ServiceConstants.PID + MavenConstants.PROPERTY_REPOSITORIES, multiRepo);
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

}

