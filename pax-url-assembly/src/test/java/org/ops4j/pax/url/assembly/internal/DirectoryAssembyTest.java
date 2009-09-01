/*
 * Copyright (c) 2009 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 *
 */
package org.ops4j.pax.url.assembly.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.io.File;
import org.junit.Test;
import org.junit.Assert;
import org.ops4j.io.FileUtils;

/**
 * {@link DirectoryAssembly} unit tests.
 *
 * @author Alin Dreghiciu
 */
public class DirectoryAssembyTest
{

    @Test
    public void scanDir()
        throws IOException
    {
        final File dir = FileUtils.getFileFromClasspath( "assemblies" ).getParentFile();
        final Set<Resource> resources = new DirectoryAssembly(
            new HashSet<String>(
                Arrays.asList(
                    dir.getCanonicalPath()
                )
            ),
            MergePolicy.FIRST
        ).scanResources();
        
    }

}