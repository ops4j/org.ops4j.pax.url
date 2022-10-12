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
package org.eclipse.aether.internal.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

public class PaxLocalRepositoryManager extends SimpleLocalRepositoryManager {

    public static final String PROPERTY_UPDATE_RELEASES = "paxUrlAether.updateReleases";

    private final UpdatePolicyAnalyzer updatePolicyAnalyzer;
    private final RemoteRepositoryManager remoteRepositoryManager;

    private final String trackingFilename;
    private final TrackingFileManager trackingFileManager;

    public PaxLocalRepositoryManager(File basedir,
                                     UpdatePolicyAnalyzer updatePolicyAnalyzer,
                                     RemoteRepositoryManager remoteRepositoryManager) {
        super(basedir, "pax-url", new DefaultLocalPathComposer());
        this.updatePolicyAnalyzer = updatePolicyAnalyzer;
        this.remoteRepositoryManager = remoteRepositoryManager;

        trackingFilename = "_pax-url-aether-remote.repositories";
        trackingFileManager = new DefaultTrackingFileManager();
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        LocalArtifactResult result = super.find(session, request);

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
        super.add(session, request);
        if (!request.getArtifact().isSnapshot()
                && (Boolean) session.getConfigProperties().get(PROPERTY_UPDATE_RELEASES)) {
            String path = getPathForLocalArtifact(request.getArtifact());
            File artifactFile = new File(getRepository().getBasedir(), path);
            File trackingFile = getTrackingFile(artifactFile);
            String repoId = request.getRepository() == null ? "" : request.getRepository().getId();

            Map<String, String> updates = new HashMap<String, String>();
            updates.put(artifactFile.getName() + ">" + repoId, "");
            trackingFileManager.update(trackingFile, updates);
        }
    }

    private File getTrackingFile(File artifactFile) {
        return new File(artifactFile.getParentFile(), trackingFilename);
    }

}
