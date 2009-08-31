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
package org.ops4j.pax.url.assembly;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.ops4j.io.FileUtils;

/**
 * {@link Handler} unit tests.
 *
 * @author Alin Dreghiciu
 */
public class HandlerTest
{

    /**
     * Protocol handler can be used.
     *
     * @throws IOException - Unexpected
     */
    @Test
    public void use()
        throws IOException
    {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        final File dir = FileUtils.getFileFromClasspath( "assemblies/simple" );
        new URL( "assembly:" + dir.getAbsolutePath() ).openStream();
    }

}
