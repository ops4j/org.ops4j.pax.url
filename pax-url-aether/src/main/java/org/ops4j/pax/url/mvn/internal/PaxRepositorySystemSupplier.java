/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.LocalPathComposer;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.http.ChecksumExtractor;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

/**
 * Extension of {@link RepositorySystemSupplier} to provide enhanced and Pax URL specific functionality to
 * the Maven Resolver if needed.
 */
public class PaxRepositorySystemSupplier extends RepositorySystemSupplier {

    private final UpdatePolicyAnalyzer updatePolicyAnalyzer;
    private final ChecksumPolicyProvider checksumPolicyProvider;
    private final RemoteRepositoryManager remoteRepositoryManager;

    public PaxRepositorySystemSupplier() {
        updatePolicyAnalyzer = super.getUpdatePolicyAnalyzer();
        checksumPolicyProvider = super.getChecksumPolicyProvider();
        remoteRepositoryManager = super.getRemoteRepositoryManager(updatePolicyAnalyzer, checksumPolicyProvider);
    }

    @Override
    protected Map<String, TransporterFactory> getTransporterFactories(Map<String, ChecksumExtractor> extractors) {
        Map<String, TransporterFactory> factories = super.getTransporterFactories(extractors);
        // explicit indication that we're not using maven-resolver-transport-wagon
        factories.put(HttpTransporterFactory.NAME, new HttpTransporterFactory(extractors));
        factories.remove("wagon");

        return factories;
    }

    @Override
    protected LocalRepositoryProvider getLocalRepositoryProvider(LocalPathComposer localPathComposer, TrackingFileManager trackingFileManager, LocalPathPrefixComposerFactory localPathPrefixComposerFactory) {
        Set<LocalRepositoryManagerFactory> localRepositoryProviders = new HashSet<>(1);
        localRepositoryProviders.add(new PaxLocalRepositoryManagerFactory(localPathComposer, trackingFileManager,
                localPathPrefixComposerFactory,
                updatePolicyAnalyzer, remoteRepositoryManager));

        return new DefaultLocalRepositoryProvider(localRepositoryProviders);
    }

    @Override
    protected UpdatePolicyAnalyzer getUpdatePolicyAnalyzer() {
        return updatePolicyAnalyzer;
    }

    @Override
    protected ChecksumPolicyProvider getChecksumPolicyProvider() {
        return checksumPolicyProvider;
    }

    @Override
    protected RemoteRepositoryManager getRemoteRepositoryManager(UpdatePolicyAnalyzer updatePolicyAnalyzer, ChecksumPolicyProvider checksumPolicyProvider) {
        return remoteRepositoryManager;
    }

}
