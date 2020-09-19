package kakao.mft.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.constant.AgentCmd;
import kakao.mft.agent.constant.AgentConst;
import kakao.mft.agent.control.AgentClient;
import kakao.mft.agent.control.AgentFileRecvServer;
import kakao.mft.agent.props.AgentProperty;

public class KaAgent {

	private final Logger logger = LoggerFactory.getLogger(KaAgent.class);
	
	private boolean running;
	private AgentFileRecvServer server;
	private AgentClient agentClient;
	private Socket socket;
	private AgentProperty prop;
	
	private KaAgent() {
		running = false;
		prop = AgentProperty.getInstance();
	}
	
	private void connectToMaster() {
		socket = getSocket(prop.getMasterHost(), prop.getMasterPort());
	}
	
	private void start() {
		logger.info("KaAgent Start...");
		connectToMaster();		
		if (socket.isConnected()) {
			agentClient = new AgentClient(socket);
			agentClient.start();
			server = new AgentFileRecvServer(agentClient, prop.getPort());
			server.start();
		} else {
			logger.error("Failed to connect KaMaster");
			stop();
			return;
		}
		await();
	}
	
	private Socket getSocket(String ip, int port) {
		int portMin = AgentProperty.getInstance().getPortMin();
		int portMax = AgentProperty.getInstance().getPortMax();
		Socket conSocket = null;
		for(int i = portMin; i < portMax; i++) {
			try {
				conSocket = new Socket(InetAddress.getByName(ip), port, InetAddress.getByName("::"), i);			
			} catch (IOException be) {}
			if (conSocket != null) {
				logger.debug("Connect to Master {}:{}", ip, port);
				break;
			}
		}
		
		return conSocket;
	}
	
	private void stop() {
		running = server.shutdown();
		logger.info("KaAgent stop...");
	}
	
	protected void await() {
		try (ServerSocket serverSocket = new ServerSocket(prop.getShutdown(), 1)){
			running = true;
			Socket shutdownSocket = null;
			InputStream is = null;
			while (running) {
				try {
					shutdownSocket = serverSocket.accept();
					shutdownSocket.setSoTimeout(10 * 1000); // Timeout 10sec
					is = shutdownSocket.getInputStream();
				} catch (AccessControlException ace) {
					continue;
				} catch (IOException e) {
					System.exit(1);
				}
								
				StringBuilder sb = new StringBuilder();
				int commandLength = AgentCmd.STOP.length();
				while (commandLength > 0 ) {				
					int ch = -1;
					try {
						ch = is.read();
					} catch (IOException e) {
						ch = -1;
					} // EOF
					if (ch < 32)
						 break;
					sb.append((char)ch);
					commandLength--;
				}
				
				try {
					is.close();
					shutdownSocket.close();
				} catch (IOException ignore) {}
				
				String command = sb.toString();
				
				if (AgentCmd.STOP.equals(command)) {
					break;
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			stop();
		}
	}
	
	private static void shutdown() {
		try (Socket shutdownSocket = new Socket(AgentConst.LOOPBACK_IP, AgentProperty.getInstance().getShutdown())) {
			OutputStream os = shutdownSocket.getOutputStream();
			for (int i = 0; i < AgentCmd.STOP.length(); i++) {
				os.write(AgentCmd.STOP.charAt(i));
			}
			os.flush();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		String command = null;
		if (args.length > 0) { command = args[0]; }
		if (AgentCmd.START.equals(command)) {
			KaAgent agent = new KaAgent();
			agent.start();
		} else
		if (AgentCmd.STOP.equals(command)) {
			shutdown();
		} else {
			System.out.println("Invalid command");
		}
	}
}
