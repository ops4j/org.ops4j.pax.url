/**
 * 
 */
package org.ops4j.pax.url.war.internal;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Achim
 *
 */
public class AbstractConnectionTest {

	/**
	 * Test method for {@link org.ops4j.pax.url.war.internal.AbstractConnection#checkJarIsLegal(java.lang.String)}.
	 */
	@Test
	public void testCheckJarIsLegal() {
		String[] servletJarNamesToTest = {
				"servlet.jar",
				"servlet-2.5.jar",
				"servlet-api.jar",
				"servlet-api-2.5.jar",
				"servlet-api-5.0.jar",
				"servlet-api-6.0.jar"
		};
		
		for (String servletName : servletJarNamesToTest) {
			assertFalse(AbstractConnection.checkJarIsLegal(servletName));
		}

		String[] jasperJarNamesToTest = {
				"jasper.jar",
				"jasper-2.5.jar",
		};

		for (String servletName : jasperJarNamesToTest) {
			assertFalse(AbstractConnection.checkJarIsLegal(servletName));
		}
		
	}

}
