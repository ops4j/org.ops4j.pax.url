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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
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
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getConfig() );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "0.7.2" );
        aetherBasedResolver.close();
    }

    @Test
    public void resolveRangeBased()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getConfig() );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "LATEST" );
        aetherBasedResolver.close();
    }
    
    @Test
    public void resolvedOnlyLocal() throws IOException{
        Settings settings = getSettings();
        URL resource = AetherTest.class.getResource("/repo2"); //$NON-NLS-1$
        settings.setLocalRepository( resource.toExternalForm() );
        Properties p = new Properties();
        MavenConfigurationImpl config = new MavenConfigurationImpl( new PropertiesPropertyResolver( p ), ServiceConstants.PID );
        config.setSettings( settings );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( config );
        try{
            //check the remote artifact is not resolved and therefore throws an exception
            try{
                aetherBasedResolver.resolve( "mvn:localrepositories://@id=foo!org.ops4j.pax.web/pax-web-api/LATEST" ); //$NON-NLS-1$
                fail( "mvn:localrepositories://@id=foo!org.ops4j.pax.web/pax-web-api/LATEST should never be resolved" ); //$NON-NLS-1$
            }catch(IOException ioe){
                //expected exception
            }
            //check local artifact is resolved
            File resolvedFile = aetherBasedResolver.resolve( "mvn:file://local.repositories@id=foo!ant/ant/1.5.1" ); //$NON-NLS-1$
        }finally{
            aetherBasedResolver.close();
        }
        
    }
    
    @Test
    public void testCachingOfRanges()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {

        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( getConfig() );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "LATEST" );
        aetherBasedResolver.close();

        // now again:
        // no repo
        aetherBasedResolver = new AetherBasedResolver( getConfig() );
        aetherBasedResolver.resolve( "org.ops4j.pax.web", "pax-web-api", "", "jar", "LATEST" );
        aetherBasedResolver.close();
    }

    private Settings getSettings()        
    {
        Settings settings = new Settings();
        settings.setLocalRepository( getCache().toURI().toASCIIString() );
        Profile centralProfile = new Profile();
        centralProfile.setId( "central" );
        Repository central = new Repository();
        central.setId( "central" );
        central.setUrl( "http://repo1.maven.org/maven2");
        centralProfile.addRepository( central );
        settings.addProfile( centralProfile );
        settings.addActiveProfile( "central" );
        return settings;
    }
    
    private MavenConfigurationImpl getConfig() {
        Properties p = new Properties();
        MavenConfigurationImpl config = new MavenConfigurationImpl( new PropertiesPropertyResolver( p ), ServiceConstants.PID );
        config.setSettings( getSettings() );
        return config;        
    }

    private File getCache() {
        File base = new File( "target" );
        base.mkdir();
        try {
            File f = File.createTempFile( "aethertest", ".dir", base );
            f.delete();
            f.mkdirs();
            LOG.info( "Caching" + " to " + f.getAbsolutePath() );
            return f;
        }
        catch( IOException exc ) {
            throw new AssertionError( "cannot create cache", exc );
        }
    }

    @Test
    public void testResolveRDF()
        throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        Settings settings = getSettings();
        Profile jbossProfile = new Profile();
        jbossProfile.setId( "jboss" );
        Repository jbossRepository = new Repository();
        jbossRepository.setId( "jboss" );
        jbossRepository.setUrl( "http://repository.jboss.org/nexus/content/repositories/thirdparty-releases");
        jbossProfile.addRepository( jbossRepository );
        settings.addProfile( jbossProfile );
        settings.addActiveProfile( "jboss" );

        MavenConfigurationImpl config = getConfig();
        config.setSettings( settings );
        AetherBasedResolver aetherBasedResolver = new AetherBasedResolver( config );
        aetherBasedResolver.resolve( "org.openrdf", "openrdf-model", "", "jar", "2.0.1" );
        aetherBasedResolver.close();
    }
}

