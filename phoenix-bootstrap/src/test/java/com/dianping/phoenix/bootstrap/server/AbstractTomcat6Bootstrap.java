package com.dianping.phoenix.bootstrap.server;

import java.io.File;
import java.net.InetAddress;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;

import com.dianping.phoenix.bootstrap.Tomcat6WebappLoader;
import com.dianping.phoenix.bootstrap.WebappProvider;

public abstract class AbstractTomcat6Bootstrap {
	protected void display(String requestUri) {
		String url = "http://localhost:" + getPort() + requestUri;
		String os = System.getProperty("os.name");
		String[] commandLine;

		if (os != null && os.startsWith("Windows")) {
			commandLine = new String[] { "rundll32", "url.dll,FileProtocolHandler", url };
		} else if (os != null && os.indexOf("Mac") >= 0) {
			commandLine = new String[] { "open", url };
		} else if (os != null && os.indexOf("Linux") >= 0) {
			commandLine = new String[] { "xdg-open", url };
		} else {
			throw new RuntimeException(String.format("Not supported OS(%s)!", System.getProperty("os.name")));
		}

		try {
			Runtime.getRuntime().exec(commandLine);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected String getCatalinaHome() {
		String catalinaHome = getClass().getResource("/tomcat6").getFile();

		return catalinaHome;
	}

	protected String getContextPath() {
		return "";
	}

	protected int getPort() {
		return 7463;
	}

	protected void startTomcat(String kernelDocBase, String appDocBase) throws Exception {
		startTomcat(null, null, kernelDocBase, appDocBase);
	}

	protected void startTomcat(WebappProvider kernelProvider, WebappProvider appProvider) throws Exception {
		String kernelDocBase = kernelProvider.getWarRoot().getCanonicalPath();
		String appDocBase = appProvider.getWarRoot().getCanonicalPath();

		startTomcat(kernelProvider, appProvider, kernelDocBase, appDocBase);
	}

	private void startTomcat(WebappProvider kernelProvider, WebappProvider appProvider, String kernelDocBase,
	      String appDocBase) throws Exception {
		Embedded container = new Embedded();

		container.setCatalinaHome(getCatalinaHome());
		container.setRealm(new MemoryRealm());

		// create host
		Host localHost = container.createHost("localHost", new File(".").getAbsolutePath());
		Tomcat6WebappLoader loader = new Tomcat6WebappLoader(getClass().getClassLoader());

		loader.setKernelWebappProvider(kernelProvider);
		loader.setApplicationWebappProvider(appProvider);
		loader.setKernelDocBase(kernelDocBase);

		Context context = container.createContext("/" + getContextPath(), appDocBase);
		context.setLoader(loader);

		// avoid write SESSIONS.ser to src/test/resources/
		StandardManager manager = new StandardManager();
		manager.setPathname(new File("target/session").getCanonicalFile().getPath());

		context.setManager(manager);
		context.setReloadable(true);

		localHost.addChild(context);

		// create engine
		Engine engine = container.createEngine();
		engine.setName("Phoenix");
		engine.addChild(localHost);
		engine.setDefaultHost(localHost.getName());
		container.addEngine(engine);

		// create http connector
		Connector httpConnector = container.createConnector((InetAddress) null, getPort(), false);

		container.addConnector(httpConnector);
		container.setAwait(true);

		// start server
		container.start();
	}
}
