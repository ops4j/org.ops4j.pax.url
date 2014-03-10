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
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.ops4j.io.FileUtils;

/**
 * JAVADOC
 *
 * @author Alin Dreghiciu
 */
public class AssemblyDescriptorParserTest
{

    @Test
    public void parseValidDescriptor()
        throws IOException
    {
        final File descriptor = FileUtils.getFileFromClasspath( "assemblies/simple.descriptor" );
        final AssemblyDescriptorParser parser = new AssemblyDescriptorParser( descriptor.toURI().toURL().toExternalForm() );
        final String manifest = parser.manifest();
        final Source[] sources = parser.sources();

        assertNotNull( "Manifest is not null", manifest );
        assertNotNull( "Parsed sources are not null", sources );
        assertThat( "Parsed sources size", sources.length, is( equalTo( 2 ) ) );

        assertNotNull( "First parsed directory path is not null", sources[ 0 ].path() );
        assertNotNull( "First parsed directory includes is not null", sources[ 0 ].includes() );
        assertNotNull( "First parsed directory excludes is not null", sources[ 0 ].excludes() );

        assertNotNull( "Second parsed directory path is not null", sources[ 1 ].path() );
        assertNotNull( "Second parsed directory includes is not null", sources[ 1 ].includes() );
        assertNotNull( "Second parsed directory excludes is not null", sources[ 1 ].excludes() );
    }

}
