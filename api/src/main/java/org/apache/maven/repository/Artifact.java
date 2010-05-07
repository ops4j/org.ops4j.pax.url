package org.apache.maven.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

/**
 * @author Benjamin Bentmann
 */
public interface Artifact
{

    String getGroupId();

    String getArtifactId();

    String getBaseVersion();

    String getVersion();

    Artifact setVersion( String version );

    boolean isSnapshot();

    String getClassifier();

    // NOTE: This represents artifactHandler.extension! The higher-level classification done with artifactHandler.type should become a property
    String getType();

    File getFile();

    Artifact setFile( File file );

    // holds characteristics of artifact which were previously controlled by artifact handler (e.g.
    // includesDependencies, addedToClasspath)
    String getProperty( String key, String defaultValue );

    Artifact clone();

}