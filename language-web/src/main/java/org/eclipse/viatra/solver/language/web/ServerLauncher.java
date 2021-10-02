/*
 * generated by Xtext 2.25.0
 */
package org.eclipse.viatra.solver.language.web;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.SessionTrackingMode;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.util.resource.Resource;

public class ServerLauncher {
	public static final String DEFAULT_LISTEN_ADDRESS = "localhost";

	public static final int DEFAULT_LISTEN_PORT = 1312;

	// Use this cookie name for load balancing.
	public static final String SESSION_COOKIE_NAME = "JSESSIONID";

	private static final Slf4jLog LOG = new Slf4jLog(ServerLauncher.class.getName());

	private final Server server;

	public ServerLauncher(InetSocketAddress bindAddress, Resource baseResource) {
		server = new Server(bindAddress);
		var handler = new ServletContextHandler();
		addSessionHandler(handler);
		addProblemServlet(handler);
		if (baseResource != null) {
			handler.setBaseResource(baseResource);
			handler.setWelcomeFiles(new String[] { "index.html" });
			addDefaultServlet(handler);
		}
		handler.addFilter(CacheControlFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		server.setHandler(handler);
	}

	private void addSessionHandler(ServletContextHandler handler) {
		var sessionHandler = new SessionHandler();
		sessionHandler.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
		sessionHandler.setSessionCookie(SESSION_COOKIE_NAME);
		handler.setSessionHandler(sessionHandler);
	}

	private void addProblemServlet(ServletContextHandler handler) {
		handler.addServlet(ProblemServlet.class, "/xtext-service/*");
	}

	private void addDefaultServlet(ServletContextHandler handler) {
		var defaultServletHolder = new ServletHolder(DefaultServlet.class);
		var isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		// Avoid file locking on Windows: https://stackoverflow.com/a/4985717
		// See also the related Jetty ticket:
		// https://github.com/eclipse/jetty.project/issues/2925
		defaultServletHolder.setInitParameter("useFileMappedBuffer", isWindows ? "false" : "true");
		handler.addServlet(defaultServletHolder, "/");
	}

	public void start() throws Exception {
		server.start();
		LOG.info("Server started " + server.getURI() + "...");
		LOG.info("Press enter to stop the server...");
		int key = System.in.read();
		if (key != -1) {
			server.stop();
		} else {
			LOG.warn("Console input is not available. "
					+ "In order to stop the server, you need to cancel process manually.");
		}
	}

	public static void main(String[] args) {
		try {
			var bindAddress = getBindAddress();
			var baseResource = getBaseResource();
			var serverLauncher = new ServerLauncher(bindAddress, baseResource);
			serverLauncher.start();
		} catch (Exception exception) {
			LOG.warn(exception);
			System.exit(1);
		}
	}

	private static String getListenAddress() {
		var listenAddress = System.getenv("LISTEN_ADDRESS");
		if (listenAddress == null) {
			return DEFAULT_LISTEN_ADDRESS;
		}
		return listenAddress;
	}

	private static int getListenPort() {
		var portStr = System.getenv("LISTEN_PORT");
		if (portStr != null) {
			return Integer.parseInt(portStr);
		}
		return DEFAULT_LISTEN_PORT;
	}

	private static InetSocketAddress getBindAddress() {
		var listenAddress = getListenAddress();
		var listenPort = getListenPort();
		return new InetSocketAddress(listenAddress, listenPort);
	}

	private static Resource getBaseResource() throws IOException, URISyntaxException {
		var baseResourceOverride = System.getenv("BASE_RESOURCE");
		if (baseResourceOverride != null) {
			// If a user override is provided, use it.
			return Resource.newResource(baseResourceOverride);
		}
		var indexUrlInJar = ServerLauncher.class.getResource("/webapp/index.html");
		if (indexUrlInJar != null) {
			// If the app is packaged in the jar, serve it.
			var webRootUri = URI.create(indexUrlInJar.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
			return Resource.newResource(webRootUri);
		}
		// Look for unpacked production artifacts (convenience for running from IDE).
		var unpackedResourcePathComponents = new String[] { System.getProperty("user.dir"), "build", "webpack",
				"production" };
		var unpackedResourceDir = new File(String.join(File.separator, unpackedResourcePathComponents));
		if (unpackedResourceDir.isDirectory()) {
			return Resource.newResource(unpackedResourceDir);
		}
		// Fall back to just serving a 404.
		return null;
	}
}
