package kakao.mft.agent.props;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.constant.AgentPropertyKey;

public class AgentProperty implements AgentPropertyKey {
	
	private final Logger logger = LoggerFactory.getLogger(AgentProperty.class);
			
	private Properties prop = new Properties();
	
	private AgentProperty() {
		load();
	}
	
	private synchronized void load() {
		String path = System.getProperty(AGENT_CONFIG);
		Path filePath = null;
		
		if (path == null) {
			try {
				if (this.getClass().getResource("/") == null) {
					URLClassLoader c = (URLClassLoader)ClassLoader.getSystemClassLoader();
					URL[] urls = c.getURLs();
					URI resolvedUri;
					for(URL url:urls) {
						resolvedUri = url.toURI().resolve("config/" + AGENT_CONFIG_FILE);
						if (Paths.get(resolvedUri).toFile().exists()) {
							filePath = Paths.get(resolvedUri);
							break;
						}
					}
				} else {
					Path rootPath = Paths.get(this.getClass().getResource("/").toURI());
					filePath = rootPath.resolve("config/" + AGENT_CONFIG_FILE);
				}
			} catch (URISyntaxException e) {}
		} else {
			filePath = Paths.get(path);
		}
		
		try {
			FileInputStream fis = new FileInputStream(filePath.toFile());
			prop.load(fis);			
		} catch (IOException ioe) {
			logger.error("Agent Property Loading Failed", ioe);
		}
	}
	
	private static class Singleton {
		private static final AgentProperty INSTANCE = new AgentProperty();
	}
	
	public static AgentProperty getInstance() {
		return Singleton.INSTANCE;
	}
	
	public String getAgentName() {
		String agentName = prop.getProperty(AGENT_NAME);
		if (agentName == null) {
			agentName = "localhost";
			try {
				agentName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				logger.error("Failed to get agent hostname", e);
			}
		}
		return agentName;
	}
	
	public String getFilePath() {
		return prop.getProperty(AGENT_FILE_PATH);
	}
	
	public String getAesKey() {
		return prop.getProperty(AES_KEY);
	}
	
	public int getPort() {
		return getPropInt(AGENT_PORT);
	}
	
	public int getPortMin() {
		return getPropInt(AGENT_PORT_MIN);
	}
	
	public int getPortMax() {
		return getPropInt(AGENT_PORT_MAX);
	}
	
	public int getShutdown() {
		return getPropInt(AGENT_SHUTDOWN);
	}
	
	public String getMasterHost() {
		return prop.getProperty(MASTER_HOST);
	}
	
	public int getMasterPort() {
		return getPropInt(MASTER_PORT);
	}
	
	private int getPropInt(String key) {
		String value = prop.getProperty(key);
		return Integer.parseInt(value);
	}
}
