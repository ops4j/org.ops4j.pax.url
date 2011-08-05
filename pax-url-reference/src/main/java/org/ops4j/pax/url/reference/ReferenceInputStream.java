/*
 * Copyright 2011 Harald Wellmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.reference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} implementation for "reference:" protocol.
 * 
 * @author Harald Wellmann (harald.wellmann@gmx.de)
 * @since 1.3.5, Aug 5, 2011
 */
public class ReferenceInputStream extends InputStream 
{
	protected URL reference;

	public ReferenceInputStream(URL reference) 
	{
		this.reference = reference;
	}

	public int read() throws IOException 
	{
		throw new IOException("cannot read from ReferenceInputStream");
	}

	public URL getReference() 
	{
		return reference;
	}
}
