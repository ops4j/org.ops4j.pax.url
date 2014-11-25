/**
 *  Copyright 2014, Guillaume Nodet.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.ops4j.pax.url.mvn;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * A maven resolver service
 *
 * @author Guillaume Nodet
 */
public interface MavenResolver extends Closeable {

    /**
     * Resolve and download a maven based url
     */
    File resolve( String url ) throws IOException;

    /**
     * Resolve and download an artifact
     */
    File resolve( String groupId, String artifactId, String classifier,
                  String extension, String version ) throws IOException;

    /**
     * Resolve the maven metadata xml for the specified groupId:artifactId:version
     */
    File resolveMetadata( String groupId, String artifactId,
                          String type, String version ) throws IOException;

    /**
     * Install the specified artifact in the local repository
     */
    void upload( String groupId, String artifactId, String classifier,
                 String extension, String version, File artifact ) throws IOException;

    /**
     * Install the specified artifact metadata in the local repository
     */
    void uploadMetadata( String groupId, String artifactId,
                         String type, String version, File artifact ) throws IOException;

}
