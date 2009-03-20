package org.codehaus.plexus.lang;

/*
 * Copyright 2001-2007 Codehaus Foundation.
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

import java.util.Locale;
import java.util.ResourceBundle;

public interface I18N
{
    public static String ROLE = I18N.class.getName();

    String ACCEPT_LANGUAGE = "Accept-Language";

    String getDefaultLanguage();

    String getDefaultCountry();

    String getDefaultBundleName();

    String[] getBundleNames();

    ResourceBundle getBundle();

    ResourceBundle getBundle( String bundleName );

    ResourceBundle getBundle( String bundleName, String languageHeader );

    ResourceBundle getBundle( String bundleName, Locale locale );

    Locale getLocale( String languageHeader );

    String getString( String key );

    String getString( String key, Locale locale );

    String getString( String bundleName, Locale locale, String key );

    String format( String key, Object arg1 );

    String format( String key, Object arg1, Object arg2 );

    String format( String bundleName, Locale locale, String key, Object arg1 );

    String format( String bundleName, Locale locale, String key, Object arg1, Object arg2 );

    String format( String bundleName, Locale locale, String key, Object[] args );
}
