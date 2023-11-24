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
package org.ops4j.pax.url.mvn.internal;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.LocalPathComposer;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;

public class PaxLocalRepositoryManagerFactory implements LocalRepositoryManagerFactory {

    private final LocalRepositoryManagerFactory delegate;

    // we need three additional "services" to be available in PaxLocalRepositoryManager
    private final UpdatePolicyAnalyzer updatePolicyAnalyzer;
    private final RemoteRepositoryManager remoteRepositoryManager;
    private final TrackingFileManager trackingFileManager;

    public PaxLocalRepositoryManagerFactory(LocalPathComposer localPathComposer, TrackingFileManager trackingFileManager,
            LocalPathPrefixComposerFactory localPathPrefixComposerFactory,
            UpdatePolicyAnalyzer updatePolicyAnalyzer, RemoteRepositoryManager remoteRepositoryManager) {
        // we can't extend org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManager,
        // so we have to delegate
        delegate = new EnhancedLocalRepositoryManagerFactory(localPathComposer, trackingFileManager, localPathPrefixComposerFactory);

        this.updatePolicyAnalyzer = updatePolicyAnalyzer;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.trackingFileManager = trackingFileManager;
    }

    @Override
    public float getPriority() {
        return 20.0f;
    }

    @Override
    public LocalRepositoryManager newInstance(RepositorySystemSession repositorySystemSession, LocalRepository localRepository) throws NoLocalRepositoryManagerException {
        LocalRepositoryManager delegate = this.delegate.newInstance(repositorySystemSession, localRepository);
        return new PaxLocalRepositoryManager(localRepository.getBasedir(), delegate,
                updatePolicyAnalyzer, remoteRepositoryManager, trackingFileManager);
    }

}
