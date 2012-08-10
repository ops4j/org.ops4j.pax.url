/*
 * Copyright (C) 2012 Andrei Pozolotin
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package amazon;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class HandlerTest extends TestBase {

	@Test
	public void test() throws Exception {

		String settings = Util.getTestSettings().getAbsolutePath();
		String localRepo = Util.getTestRepo().getAbsolutePath();

		System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

		System.setProperty("org.ops4j.pax.url.mvn.settings", settings);

		System.setProperty("org.ops4j.pax.url.mvn.localRepository", localRepo);

		System.setProperty("org.ops4j.pax.url.mvn.certificateCheck", "false");

		System.setProperty("org.ops4j.pax.url.mvn.useFallbackRepositories",
				"false");

		InputStream input = new URL("mvn:org.ops4j.base/ops4j-base-lang/1.1.0")
				.openStream();

		// Thread.sleep(100 * 1000);

		input.close();

	}

}
