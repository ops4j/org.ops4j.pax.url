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

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.ops4j.io.FileUtils;

/**
 * {@link ResourceAssembly} unit tests.
 *
 * @author Alin Dreghiciu
 */
public class DirectoryAssembyTest
{

    @Test
    public void scanDir()
        throws IOException
    {
        final File dir = FileUtils.getFileFromClasspath( "assemblies/simple" );
        final ResourceAssembly assembly = new ResourceAssembly(
            new Source[]{ new PathEncodedSource( dir.getCanonicalPath() ) }, MergePolicy.FIRST
        );
    }

    @Test
    public void scanZip()
        throws IOException
    {
        final File dir = FileUtils.getFileFromClasspath( "assemblies/simple.zip" );
        final ResourceAssembly assembly = new ResourceAssembly(
            new Source[]{ new PathEncodedSource( dir.getCanonicalPath() ) }, MergePolicy.FIRST
        );
    }

}