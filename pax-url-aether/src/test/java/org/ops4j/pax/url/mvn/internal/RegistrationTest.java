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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.Test;
import org.ops4j.io.FileUtils;
import org.osgi.framework.BundleContext;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class RegistrationTest {

    @Test
    @SuppressWarnings("unchecked")
    public void registerWithoutConfigAdmin() {
        final Properties properties = new Properties();
        properties.setProperty("org.ops4j.pax.url.mvn.localRepository", "target/repository");

        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyObject(String.class))).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                String key = (String) getCurrentArguments()[0];
                return properties.getProperty(key);
            }
        }).anyTimes();

        expect(context.registerService(same("org.osgi.service.url.URLStreamHandlerService"),
                anyObject(), anyObject(Dictionary.class))).andReturn(null);
        expect(context.registerService(same("org.osgi.service.cm.ManagedService"),
                anyObject(), anyObject(Dictionary.class))).andReturn(null);
        Capture<Dictionary<String, Object>> registrationProperties = new Capture<>();
        expect(context.registerService(same("org.ops4j.pax.url.mvn.MavenResolver"),
                anyObject(), capture(registrationProperties))).andReturn(null);

        replay(context);

        Activator activator = new Activator();
        activator.start(context);
        verify(context);

        assertThat((String)registrationProperties.getValue().get("configuration"), equalTo("bundlecontext"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void registerWithoutConfigAdminAndMalformedSettings() throws FileNotFoundException {
        final Properties properties = new Properties();
        properties.setProperty("org.ops4j.pax.url.mvn.localRepository", "target/repository");
        File malformedSettings = FileUtils.getFileFromClasspath("configuration/malformed-settings.xml");
        properties.setProperty("org.ops4j.pax.url.mvn.settings", malformedSettings.getAbsolutePath());

        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyObject(String.class))).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                String key = (String) getCurrentArguments()[0];
                return properties.getProperty(key);
            }
        }).anyTimes();

        expect(context.registerService(same("org.osgi.service.cm.ManagedService"),
                anyObject(), anyObject(Dictionary.class))).andReturn(null);

        replay(context);

        Activator activator = new Activator();
        activator.start(context);
        verify(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void registerWithConfigAdmin() throws NoSuchFieldException, IllegalAccessException {
        final Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("org.ops4j.pax.url.mvn.localRepository", "target/repository");

        BundleContext context = createMock(BundleContext.class);

        expect(context.registerService(same("org.osgi.service.url.URLStreamHandlerService"),
                anyObject(), anyObject(Dictionary.class))).andReturn(null);
        Capture<Dictionary<String, Object>> registrationProperties = new Capture<>();
        expect(context.registerService(same("org.ops4j.pax.url.mvn.MavenResolver"),
                anyObject(), capture(registrationProperties))).andReturn(null);

        replay(context);

        Activator activator = new Activator();
        Field f = Activator.class.getDeclaredField("m_bundleContext");
        f.setAccessible(true);
        f.set(activator, context);

        activator.updated(properties);
        verify(context);

        assertThat((String)registrationProperties.getValue().get("configuration"), equalTo("configadmin"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void registerWithoutRequiredConfigAdmin() {
        final Properties properties = new Properties();
        properties.setProperty("org.ops4j.pax.url.mvn.localRepository", "target/repository");
        properties.setProperty("org.ops4j.pax.url.mvn.requireConfigAdminConfig", "true");

        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyObject(String.class))).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                String key = (String) getCurrentArguments()[0];
                return properties.getProperty(key);
            }
        }).anyTimes();

        Capture<Object> urlStreamHandlerService = new Capture<>();
        Capture<Object> mavenResolver = new Capture<>();
        expect(context.registerService(same("org.osgi.service.url.URLStreamHandlerService"),
                capture(urlStreamHandlerService), anyObject(Dictionary.class))).andReturn(null).anyTimes();
        expect(context.registerService(same("org.osgi.service.cm.ManagedService"),
                anyObject(), anyObject(Dictionary.class))).andReturn(null);
        expect(context.registerService(same("org.ops4j.pax.url.mvn.MavenResolver"),
                capture(mavenResolver), anyObject(Dictionary.class))).andReturn(null).anyTimes();

        replay(context);

        Activator activator = new Activator();
        activator.start(context);
        verify(context);

        assertFalse("org.osgi.service.url.URLStreamHandlerService should not be registered",
                urlStreamHandlerService.hasCaptured());
        assertFalse("org.ops4j.pax.url.mvn.MavenResolver should not be registered",
                mavenResolver.hasCaptured());
    }

}
