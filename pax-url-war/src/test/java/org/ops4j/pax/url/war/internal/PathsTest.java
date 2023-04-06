/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.war.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathsTest {

    @Test
    public void pathSanity() {
        Path jar = Paths.get("/WEB-INF/lib/a.jar");
        assertEquals("/WEB-INF/lib", jar.getParent().toString());

        Path otherJar = jar.getParent().resolve("../lib2/b.jar").normalize();
        assertEquals("/WEB-INF/lib2/b.jar", otherJar.toString());

        Path illegalJar1 = jar.getParent().resolve("../../lib2/b.jar").normalize();
        assertEquals("/lib2/b.jar", illegalJar1.toString());

        Path illegalJar2 = jar.getParent().resolve("../../../lib2/b.jar").normalize();
        assertEquals("/lib2/b.jar", illegalJar2.toString());
    }

}
