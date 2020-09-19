package kakao.mft.agent.control;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.task.AgentFileRecvTask;

public class AgentFileRecvServer extends Thread {
	
	private final Logger logger = LoggerFactory.getLogger(AgentFileRecvServer.class);
	
	private AgentClient agentClient;
	private int port;
	private ServerSocket serverSocket;
	private ExecutorService pool;

	private ControllerStatus status;
	public AgentFileRecvServer(AgentClient agentClient, int port) {
		this(agentClient, port, 2);
	}

	public AgentFileRecvServer(AgentClient agentClient, int port, int threads) {
		this.agentClient = agentClient;
		this.port = port;
		pool = Executors.newFixedThreadPool(threads);
		status = new ControllerStatus();
		this.setName("AgentFileRecvServer-Thread-"+this.getId());
	}
	
	public boolean shutdown() {
		status.stop();
		pool.shutdown();
		while(!pool.isTerminated()) {}
		try {
			serverSocket.close();			
		} catch (IOException ignore) {}
		return pool.isTerminated();
	}
	
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port, 100);
			logger.info("KaAgent file receive server start {}", serverSocket);
			while(status.isRun()) {
				Socket socket = serverSocket.accept();
				Callable<Void> transferTask = new AgentFileRecvTask(agentClient, socket, status);
				pool.submit(transferTask);
			}
		} catch (IOException ioe) {
			logger.error("KaAgent file receive server start error", ioe);
		}
 	}
}
