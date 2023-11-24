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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

public class PaxLocalRepositoryManager implements LocalRepositoryManager {

    public static final String PROPERTY_UPDATE_RELEASES = "paxUrlAether.updateReleases";

    private final LocalRepositoryManager delegate;

    private final UpdatePolicyAnalyzer updatePolicyAnalyzer;
    private final RemoteRepositoryManager remoteRepositoryManager;
    private final TrackingFileManager trackingFileManager;

    private final String trackingFilename;

    public PaxLocalRepositoryManager(File basedir, LocalRepositoryManager delegate,
            UpdatePolicyAnalyzer updatePolicyAnalyzer, RemoteRepositoryManager remoteRepositoryManager, TrackingFileManager trackingFileManager) {
        this.delegate = delegate;

        this.updatePolicyAnalyzer = updatePolicyAnalyzer;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.trackingFileManager = trackingFileManager;

        this.trackingFilename = "_pax-url-aether-remote.repositories";
    }

    @Override
    public LocalRepository getRepository() {
        return delegate.getRepository();
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        return delegate.getPathForLocalArtifact(artifact);
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteArtifact(artifact, repository, context);
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        return delegate.getPathForLocalMetadata(metadata);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteMetadata(metadata, repository, context);
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        LocalArtifactResult result = delegate.find(session, request);

        if (result.isAvailable()
                && !request.getArtifact().isSnapshot()
                && (Boolean) session.getConfigProperties().get(PROPERTY_UPDATE_RELEASES)) {
            // check if we should force download
            File trackingFile = getTrackingFile(result.getFile());
            Properties props = trackingFileManager.read(trackingFile);
            if (props != null) {
                String localKey = result.getFile().getName() + ">";
                if (props.get(localKey) == null) {
                    // artifact is available, but doesn't origin from local repository
                    for (RemoteRepository repo : request.getRepositories()) {
                        String remoteKey = result.getFile().getName() + ">" + repo.getId();
                        if (props.get(remoteKey) != null) {
                            // artifact origins from remote repository, check policy
                            long lastUpdated = result.getFile().lastModified();
                            RepositoryPolicy policy = remoteRepositoryManager.getPolicy(session, repo, true, false);
                            if (updatePolicyAnalyzer.isUpdatedRequired(session, lastUpdated, policy.getUpdatePolicy())) {
                                result.setAvailable(false);
                                // needed for non SNAPSHOTs.
                                // If we don't null, PeekTaskRunner will be used instead of GetTaskRunner
                                result.setFile(null);
                                result.setRepository(repo);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        delegate.add(session, request);

        if (!request.getArtifact().isSnapshot()
                && (Boolean) session.getConfigProperties().get(PROPERTY_UPDATE_RELEASES)) {
            String path = getPathForLocalArtifact(request.getArtifact());
            File artifactFile = new File(getRepository().getBasedir(), path);
            File trackingFile = getTrackingFile(artifactFile);
            String repoId = request.getRepository() == null ? "" : request.getRepository().getId();

            Map<String, String> updates = new HashMap<>();
            updates.put(artifactFile.getName() + ">" + repoId, "");
            trackingFileManager.update(trackingFile, updates);
        }
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        return delegate.find(session, request);
    }

    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        delegate.add(session, request);
    }

    private File getTrackingFile(File artifactFile) {
        return new File(artifactFile.getParentFile(), trackingFilename);
    }

}
