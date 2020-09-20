package kakao.mft.agent.control;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.props.AgentProperty;
import kakao.mft.agent.task.AgentFileSendTask;

public class AgentClient extends Thread {
	
	private final Logger logger = LoggerFactory.getLogger(AgentClient.class);
	
	private Socket socket;
	
	private InputStream is;
	private OutputStream os;
	private DataInputStream dis;
	private DataOutputStream dos;
	
	private String agentName;
	
	public AgentClient(Socket socket) {
		this.socket = socket;
		this.agentName = AgentProperty.getInstance().getAgentName();
		this.setName("AgentClient-Thread-"+this.getId());
	}
	
	@Override
	public void run() {
		try {
			is = socket.getInputStream();
			os = socket.getOutputStream();
			dis = new DataInputStream(is);
			dos = new DataOutputStream(os);
		} catch (IOException ioe) {
			logger.error("KaMaster connection error {}", socket, ioe);
		}
		
		try {
			if (isSocketAlive()) {
				logger.debug("KaMaster connection initialize {}", socket);
				dos.writeUTF("init");
				dos.writeUTF(agentName);
				dos.writeInt(AgentProperty.getInstance().getPort());
			} else {
				
			}
			while(isSocketAlive()) {
				String severMsg = dis.readUTF();
				if (severMsg.startsWith("files:")) {
					getFileList(Integer.parseInt(severMsg.substring(6)));
				} else 
				if ("send".equals(severMsg)){
					String host = dis.readUTF();
					int port = dis.readInt();
					String fileName = dis.readUTF();
					Thread t = new AgentFileSendTask(this, host, port, fileName);
					t.start();
				}
			} 
		} catch (IOException e) {
			logger.error("Agent client error", e);
		}
	}
	
	public void getFileList(int page) {
		Path path = Paths.get(AgentProperty.getInstance().getFilePath());
		File rootPath = path.toFile();
		File[] files = rootPath.listFiles();
		try {
			dos.writeUTF("files");
			dos.writeInt(files == null?0:files.length);
			for(File file:files) {
				dos.writeUTF(file.getName());
			}
		} catch (IOException ioe) {
			logger.error("Failed get file list", ioe);
		}
	}
	
	public void success(String message) {
		try {
			dos.writeUTF(message);
		} catch (IOException ioe) {
			logger.error("File transfer success message send failed.");
		}
	}
	
	public String getAgentName() {
		return agentName;
	}
	
	public boolean isSocketAlive() {
		return socket.isConnected() && !socket.isClosed();
	}
}
