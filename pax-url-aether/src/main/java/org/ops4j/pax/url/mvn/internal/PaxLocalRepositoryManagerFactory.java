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
import org.eclipse.aether.internal.impl.PaxLocalRepositoryManager;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;

public class PaxLocalRepositoryManagerFactory extends SimpleLocalRepositoryManagerFactory {

    private Logger logger;
    private UpdatePolicyAnalyzer updatePolicyAnalyzer;
    private RemoteRepositoryManager remoteRepositoryManager;

    @Override
    public void initService(ServiceLocator locator) {
        super.initService(locator);
        updatePolicyAnalyzer = locator.getService(UpdatePolicyAnalyzer.class);
        remoteRepositoryManager = locator.getService(RemoteRepositoryManager.class);
    }

    @Override
    public LocalRepositoryManager newInstance(RepositorySystemSession session, LocalRepository repository) {
        return new PaxLocalRepositoryManager(repository.getBasedir(),
                updatePolicyAnalyzer, remoteRepositoryManager).setLogger(logger);
    }

    @Override
    public SimpleLocalRepositoryManagerFactory setLoggerFactory(LoggerFactory loggerFactory) {
        SimpleLocalRepositoryManagerFactory localRepositoryManagerFactory = super.setLoggerFactory(loggerFactory);
        this.logger = NullLoggerFactory.getSafeLogger(loggerFactory, PaxLocalRepositoryManagerFactory.class);
        return localRepositoryManagerFactory;
    }

}
