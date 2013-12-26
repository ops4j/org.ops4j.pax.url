/*
 * Copyright 2013 Harald Wellmann
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
 *
 */

package org.ops4j.pax.url.mvn;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.junit.Test;
import org.ops4j.pax.url.mvn.internal.ManualWagonProvider;

/**
 * Tests Eclipse Aether as is, demonstrating how to embed it into Pax URL.
 * 
 * @author Harald Wellmann
 *
 */
public class DirectAetherTest {

    @Test
    public void resolveArtifact() throws DependencyCollectionException,
        DependencyResolutionException {
        
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator
            .addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.setServices( WagonProvider.class, new ManualWagonProvider( 10000 ) );
        locator.addService( TransporterFactory.class, WagonTransporterFactory.class );
        RepositorySystem system = locator.getService( RepositorySystem.class );

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        Dependency dependency = new Dependency( new DefaultArtifact(
            "org.apache.maven:maven-profile:2.2.1" ), "compile" );
        RemoteRepository central = new RemoteRepository.Builder( "central", "default",
            "http://repo1.maven.org/maven2/" ).build();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( dependency );
        collectRequest.addRepository( central );
        DependencyNode node = system.collectDependencies( session, collectRequest ).getRoot();

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setRoot( node );

        system.resolveDependencies( session, dependencyRequest );

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept( nlg );

        String[] jars = { "maven-profile-2.2.1.jar", //
            "maven-model-2.2.1.jar", //
            "plexus-utils-1.5.15.jar", //
            "plexus-interpolation-1.11.jar", //
            "plexus-container-default-1.0-alpha-9-stable-1.jar", //
            "junit-3.8.1.jar", //
            "classworlds-1.1.jar" // 
        };

        assertThat( nlg.getFiles().size(), is( jars.length ) );
        int i = 0;
        for( File file : nlg.getFiles() ) {
            assertThat( file.getName(), is( jars[i++] ) );
        }
    }
}
