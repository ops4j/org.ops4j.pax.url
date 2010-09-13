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
package org.ops4j.pax.url.aether.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Aether based, drop in replacement for mvn protocol
 */
public class AetherBasedResolver {

    private static final Log LOG = LogFactory.getLog(AetherBasedResolver.class);

    final private String m_localRepo;
    final private String[] m_repositories;
    final private RepositorySystem m_repoSystem;

    public AetherBasedResolver(String local, String[] repos) {
        m_localRepo = local;
        m_repositories = repos;
        m_repoSystem = newRepositorySystem();
    }

    public InputStream resolve(String groupId, String artifactId, String extension, String version) {

        try {
            RepositorySystemSession session = newSession(m_repoSystem);

            Dependency dependency =
                    new Dependency(new DefaultArtifact(groupId, artifactId, extension, version), "provided");

            LOG.info("Resolving using Aether Session: " + dependency.toString() );
            CollectRequest collectRequest = new CollectRequest();
            
            int i = 0;
            for (String repos : m_repositories) {
                RemoteRepository central = new RemoteRepository("repos" + i, "default", repos);
                collectRequest.addRepository(central);
            }

            collectRequest.setRoot(dependency);
            DependencyNode node = m_repoSystem.collectDependencies(session, collectRequest).getRoot();

            File resolved = m_repoSystem.resolveDependencies(session, node, null).get(0).getArtifact().getFile();
        
            return new FileInputStream(resolved);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (DependencyCollectionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }

    }

    private RepositorySystemSession newSession(RepositorySystem system) {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository(m_localRepo);

        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        return session;
    }

    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();

        locator.setServices(WagonProvider.class, new ManualWagonProvider());

        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);

        locator.setService(Logger.class, LogAdapter.class );

        /**
        locator.setService(VersionResolver.class, DefaultVersionResolver.class);
        locator.setService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.setService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
**/

        return locator.getService(RepositorySystem.class);
    }
}
