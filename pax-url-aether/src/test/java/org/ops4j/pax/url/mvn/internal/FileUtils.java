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
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;

public class FileUtils {

    public static File getFileFromClasspath(String filePath) throws FileNotFoundException {
        try {
            URL fileURL = FileUtils.class.getClassLoader().getResource(filePath);
            if (fileURL == null) {
                throw new FileNotFoundException("File [" + filePath + "] could not be found in classpath");
            } else {
                return new File(fileURL.toURI());
            }
        } catch (URISyntaxException var2) {
            throw new FileNotFoundException("File [" + filePath + "] could not be found: " + var2.getMessage());
        }
    }

}
