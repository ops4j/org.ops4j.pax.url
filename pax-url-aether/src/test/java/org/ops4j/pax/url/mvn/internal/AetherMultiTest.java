/*
 * Copyright (C) 2010 Toni Menzel
 * Copyright (C) 2019 Devin Avery
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.replay;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.settings.Settings;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.config.MavenConfiguration;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import org.ops4j.pax.url.mvn.internal.config.MavenRepositoryURL;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simply playing with mvn api.
 *
 * NOTE: For some of our tests which deal with file order checking we need
 * to leverage a more powerful mocking framework. This is required because
 * we need to make the order in which files are returned from File.listFiles()
 * predictable so we can ensure that our production code works as expected.
 */
//Because we are changing class loader logic we need to run with the PowerMock
//test runner instead of the default junit runner.
@RunWith(PowerMockRunner.class)
//This is the class in which we want to override creation of File objects.
//The intercept only happens in the tests where we tell it too.
@PrepareForTest( {MavenRepositoryURL.class})
//The following ignores are required to allow Logger classes to load up
//during unit testing without errors.
@PowerMockIgnore({"javax.xml.*",
                  "javax.net.ssl.*",
                  "org.xml.sax.*",
                  "org.w3c.dom.*",
                  "org.springframework.context.*",
                  "org.apache.log4j.*",
                  "javax.xml.parsers.*",
                  "org.apache.http.ssl.*",
                  "org.apache.http.conn.ssl.*",
                  "com.sun.org.apache.xerces.*"})
public class AetherMultiTest {

    private static Logger LOG = LoggerFactory.getLogger( AetherMultiTest.class );

    @Test
    public void resolveArtifactUsingMulti()
        throws Exception
    {
        AetherBasedResolver aetherBasedResolver =
            new AetherBasedResolver(
                    getDummyConfigRemoveRepo("repomulti" ));
        aetherBasedResolver.resolve( "ant", "ant", "", "jar", "1.5.1" );
        aetherBasedResolver.close();
    }

    @Test
    public void resolveArtifactUsingMultiIgnoreFiles()
        throws Exception
    {
        AetherBasedResolver aetherBasedResolver =
            new AetherBasedResolver(
                    getDummyConfigRemoveRepo("repomulti_with_file"));
        aetherBasedResolver.resolve( "ant", "ant", "", "jar", "1.5.1" );
        aetherBasedResolver.close();
    }

    /**
     * Validate that the @multi forces the files to be returned in a predictable
     * order (namely alphabetical)
     * @throws Exception
     */
    @Test
    public void resolveArtifactUsingMultiAssertOrder()
        throws Exception
    {
        //Tell PowerMock to override the "File.listFiles()" methods
        forceFileToReturnListFilesInReverseOrder();

        //The test: ensure this multi repo returns folders in expected order.
        AetherBasedResolver aetherBasedResolver =
                new AetherBasedResolver(
                        getDummyConfigRemoveRepo("repomulti_ordered"));
        List<RemoteRepository> repositories =
                aetherBasedResolver.getRepositories();
        assertEquals("Expected only 2 repositories returned",
                     2, repositories.size());
        assertTrue("Expected mykar1 first",
                    repositories.get(0).getUrl().contains("mykar1"));
        assertFalse("Didn't expect mykar2 in first url",
                     repositories.get(0).getUrl().contains("mykar2"));

        assertTrue("Expected mykar2 second",
                    repositories.get(1).getUrl().contains("mykar2"));
        assertFalse("Didn't expect mykar1 in second url",
                    repositories.get(1).getUrl().contains("mykar1"));
        aetherBasedResolver.close();
    }

    @Test
    public void resolveArtifactSelectDefaultRepositories() throws Exception
    {
        //Tell PowerMock to override the "File.listFiles()" methods
        forceFileToReturnListFilesInReverseOrder();

        //The test: ensure this multi repo returns folders in expected order.
        AetherBasedResolver aetherBasedResolver =
            new AetherBasedResolver(
                getDummyConfig("repomulti_ordered",
                               ServiceConstants.PROPERTY_DEFAULT_REPOSITORIES));
        List<LocalRepository> repositories =
                aetherBasedResolver.selectDefaultRepositories();
        assertEquals("Expected only 2 repositories returned",
                     2, repositories.size());
        assertTrue("Expected mykar1 first",
                    repositories.get(0)
                                .getBasedir()
                                .getAbsolutePath().contains("mykar1"));
        assertFalse("Didn't expect mykar2 in first url",
                     repositories.get(0)
                                  .getBasedir()
                                  .getAbsolutePath().contains("mykar2"));

        assertTrue("Expected mykar2 second",
                   repositories.get(1)
                                  .getBasedir()
                                  .getAbsolutePath().contains("mykar2"));
        assertFalse("Didn't expect mykar1 in second url",
                    repositories.get(1)
                                .getBasedir()
                                .getAbsolutePath().contains("mykar1"));
        aetherBasedResolver.close();
    }

    /**
     * Using PowerMock, we override the creation of File objects to return
     * our Mocked File object. Our mocked File is the same as a file Object,
     * except it returns the names of files to the "listFiles()" method in a
     * predictable (reverse alphabetica) order.
     * @throws Exception
     */
    private void forceFileToReturnListFilesInReverseOrder() throws Exception {
        /**
         *  By creating here we get default class loaded which is the normal
         *  java.io.File
         */
        final UnmodifiedFileFactory fileFactory = new UnmodifiedFileFactory(){
            @Override
            public File newFile(String p) {
                return new File(p);
            }
        };

        /**
         * We are using PowerMock to basically intercept all "new" calls
         * to java.io.File. We do this because we want to interject a partial
         * mock that provides a predicatableBAD order to the file names returned
         * from listFiles, so we can thus make sure that our sorting logic works.
         */
        final Capture<String> createCapture = Capture.newInstance();
        PowerMock.expectNew(File.class, new Class<?>[]{String.class},
                            capture(createCapture) ).andStubAnswer(
            new IAnswer<File>() {
                @Override
                public File answer() throws Throwable {
                    return createMockFile(fileFactory, createCapture.getValue());
                }
            }
        );
        /** Because we are intercept "new" calls, need to replay the class **/
        replay(File.class);
    }

    /**
     * PowerMock intercepts all calls to "new File" so that we
     * can change the default behavior.
     *
     * The problem is we only want to change the behavior slightly in our
     * tests but still call the real method. In order to create an unmodified
     * File object in our mock, we need to create an inner class that has an
     * unmodified class loader.
     * @author Devin Avery
     *
     */
    private static interface UnmodifiedFileFactory{
        public File newFile(String p);
    }

    /**
     * Sorts the files in reverse alphabetical order by their name().
     * @param source File array to reverse sort.
     */
    private static void reverseSortFiles( File[] source ){
        Arrays.sort(source, new Comparator<File>(){
            @Override
            public int compare(File arg0, File arg1) {
                return arg0.getName().compareTo(arg1.getName()) * -1;
            }
        });
    }

    /**
     * Creates a mock java.io.File object which is the same as the original
     * java.io.File, EXCEPT it returns calls to listFiles() in a predictable,
     * reverse alphabetical order.
     *
     * This is necessary since java.io.listFiles() doesn't guarantee the order
     * which files are returned and thus we can't truly know if the order
     * we got back is because we have working code, or just got lucky. By
     * mocking out the behavior to always return files opposite of what we
     * want we can now guarantee the code fix.
     * @param factory The file factory which is capable of creating unmodified
     * files (i.e. instantiated BEFORE powermock modifies classloader)
     * @param fileName The argument for the File object.
     * @return A java.io.File object which is the same as the origin java.io.File
     * object except that listFiles is guaranteed to return files in reverse
     * alphabetical order.
     */
    private File createMockFile(UnmodifiedFileFactory factory, final String fileName) {
        final File realFile = factory.newFile(fileName);

        File mockFile = PowerMock.createPartialMock(File.class,
                                                    new String[]{"listFiles"},
                                                    fileName );

        final Capture<FilenameFilter> filenameFilter = Capture.newInstance();
        expect(mockFile.listFiles(capture(filenameFilter))).andStubAnswer(
            new IAnswer<File[]>(){
                @Override
                public File[] answer() throws Throwable {
                    File[] listFiles = realFile.listFiles(filenameFilter.getValue());
                    reverseSortFiles(listFiles);
                    return listFiles;
                }
            }
        );

        final Capture<FileFilter> filter = Capture.newInstance();
        expect(mockFile.listFiles(capture(filter))).andStubAnswer(
            new IAnswer<File[]>(){
                @Override
                public File[] answer() throws Throwable {
                    File[] listFiles = realFile.listFiles(filter.getValue());
                    reverseSortFiles(listFiles);
                    return listFiles;
                }
            }
        );
        expect(mockFile.listFiles()).andStubAnswer(new IAnswer<File[]>() {
            @Override
            public File[] answer() throws Throwable {
                File[] listFiles = realFile.listFiles();
                reverseSortFiles(listFiles);
                return listFiles;
            }
        });
        replay(mockFile);
        return mockFile;
    }

    private MavenConfiguration getDummyConfigRemoveRepo( String folder_name )
            throws IOException
    {
        return getDummyConfig(folder_name,
                              ServiceConstants.PROPERTY_REPOSITORIES);
    }

    private MavenConfiguration getDummyConfig( String folder_name,
                                               String repoType )
         throws IOException
    {
         Properties p = new Properties();
         String localRepo = getCache().toURI().toASCIIString();
         p.setProperty( ServiceConstants.PID + "." +
                         ServiceConstants.PROPERTY_LOCAL_REPOSITORY, localRepo );

         File target = new File("target/test-classes/" + folder_name);
         assertTrue("Can not find test repo " + target.toURI().toString(),
                     target.isDirectory());
         String multiRepo = target.toURI().toString() + "@id=multitest@multi";
         p.setProperty( ServiceConstants.PID + "." + repoType, multiRepo);

         MavenConfigurationImpl config = new MavenConfigurationImpl(
                 new PropertiesPropertyResolver( p ), ServiceConstants.PID );
         Settings settings = new Settings();
         settings.setLocalRepository( localRepo );
         config.setSettings( settings );
         return config;
    }

    private File getCache()
        throws IOException
    {
        File base = new File( "target" );
        base.mkdir();
        File f = File.createTempFile( "aethertest", ".dir", base );
        f.delete();
        f.mkdirs();
        LOG.info( "Caching" + " to " + f.getAbsolutePath() );
        return f;
    }
}
