/*
 * Copyright (C) 2010 Toni Menzel
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

import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.transport.wagon.WagonProvider;

/**
 * Simplistic wagon provider
 */
public class ManualWagonProvider
    implements WagonProvider
{

    private int timeout;

    public ManualWagonProvider(int timeout) {
        this.timeout = timeout;
    }

    public Wagon lookup( String roleHint )
        throws Exception
    {
        if( "file".equals( roleHint ) )
        {
            return new FileWagon();
        }
        else if( "http".equals( roleHint ) )
        {
            LightweightHttpWagon lightweightHttpWagon = new LightweightHttpWagon();
            lightweightHttpWagon.setTimeout(timeout );
            lightweightHttpWagon.setAuthenticator( new LightweightHttpWagonAuthenticator() );
            return lightweightHttpWagon;
        }else if( "https".equals( roleHint ) )
        {
            LightweightHttpWagon lightweightHttpWagon = new LightweightHttpsWagon() {

				/** 
				 * construct equivalent of
				 *  
				 * @plexus.configuration 
				 * private Properties httpHeaders;
				 * 
				 * which is injected from settings.xml during normal maven invocation 
				 *
						<server>
							<id>server-id</id>
							<configuration>
								<httpHeaders>
									<property>
										<name>User-Agent</name>
										<value>magic-value</value>
									</property>
								</httpHeaders>
							</configuration>
						</server>
				 * 
				 * which is one way for AWS S3 https authentication via "aws:UserAgent"
				 * http://docs.amazonwebservices.com/AmazonS3/latest/dev/UsingIAMPolicies.html#AmazonS3PolicyKeys
				 * 
				 * see sample AWS S3 policy in 
				 * /pax-url-aether/src/test/resources/amazon/s3-example-policy-with-user-agent.json
				 * 
				 */
				@Override
				public void fillInputData(final InputData inputData) throws 
						TransferFailedException,
						ResourceDoesNotExistException, 
						AuthorizationException {

				        AuthenticationInfo authInfo = getAuthenticationInfo();
                                        if (authInfo != null) {
				            final String username = authInfo.getUserName();
				            final String password = authInfo.getPassword();

				            if(username != null && password != null) {
				                getHttpHeaders().put(username, password);
				            }
                                        }
					
					super.fillInputData(inputData);

				}
			};
            lightweightHttpWagon.setTimeout(timeout);
            lightweightHttpWagon.setAuthenticator( new LightweightHttpWagonAuthenticator() );
            return lightweightHttpWagon;
        }
        
        return null;
        
    }

    public void release( Wagon wagon )
    {
    }

}
