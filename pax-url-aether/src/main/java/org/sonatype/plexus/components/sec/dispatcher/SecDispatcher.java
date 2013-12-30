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


/**
 * This component descrypts a string, passed to it
 * 
 * @author Oleg Gusakov
 */
public interface SecDispatcher
{
    public static String ROLE = SecDispatcher.class.getName();
    
    public static final String [] SYSTEM_PROPERTY_MASTER_PASSWORD = new String [] {"settings.master.password","settings-master-password"};
    
    public static final String [] SYSTEM_PROPERTY_SERVER_PASSWORD = new String [] {"settings.server.password","settings-server-password"};

    /**
     * decrypt given encrypted string
     * 
     * @param str
     * @return decrypted string
     * @throws SecDispatcherException
     */
    String decrypt( String str )
    throws SecDispatcherException;
}
