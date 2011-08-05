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
package org.ops4j.pax.url.reference.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.ops4j.pax.url.reference.ReferenceInputStream;

/**
 * {@link URLStreamHandler} implementation for "reference:" protocol.
 * 
 * @author Harald Wellmann (harald.wellmann@gmx.de)
 * @since 1.3.5, Aug 5, 2011
 */
public class ReferenceUrlConnection extends URLConnection {
	protected URL reference;

	public ReferenceUrlConnection(URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
		if (!connected) {
			reference = new Parser(url.getPath()).getUrl();
		}

	}

	public boolean getDoInput() {
		return true;
	}

	public boolean getDoOutput() {
		return false;
	}

	public InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}

		return new ReferenceInputStream(reference);
	}
}
