/**
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
 
package org.sonatype.plexus.components.sec.dispatcher;

import java.util.Map;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id: PasswordDecryptor.java 882 2009-02-12 22:04:10Z oleg $
 *
 */
public interface PasswordDecryptor
{
    public static String ROLE = PasswordDecryptor.class.getName();

    /**
     * decrypt given encrypted string
     * 
     * @param str - string to decrypt
     * @param attributes - string attributes
     * @param config - configuration from settings-security.xml, if any
     * @return decrypted string
     * 
     * @throws SecDispatcherException
     */
    String decrypt( String str, Map attributes, Map config )
    throws SecDispatcherException;
}
