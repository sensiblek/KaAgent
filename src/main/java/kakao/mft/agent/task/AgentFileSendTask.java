package kakao.mft.agent.task;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.crypto.CipherOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.control.AgentClient;
import kakao.mft.agent.props.AgentProperty;
import kakao.mft.agent.security.AESEncUtil;

public class AgentFileSendTask extends Thread {
	
	private final Logger logger = LoggerFactory.getLogger(AgentFileSendTask.class);
	
	private AgentClient agentClient;
	private Socket socket;
	private InputStream is;
	private OutputStream os;
	
	private String host;
	private int port;
	private String fileName;
	
	public AgentFileSendTask(AgentClient agentClient, String host, int port, String fileName) {
		this.agentClient = agentClient;
		this.host = host;
		this.port = port;
		this.fileName = fileName;
		this.setName("AgentFileSendTask-Thread-"+this.getId());
	}
	
	// Outbound 포트 확인. 지정 범위내의 포트로만 오픈한다.
	private void createConnection(int localPort) {
		try {
			socket = new Socket(InetAddress.getByName(host), port, InetAddress.getByName("::"), localPort);			
		} catch (IOException be) {
			socket = null;
		}
	}
	
	public void connect() throws IOException {
		int portMin = AgentProperty.getInstance().getPortMin();
		int portMax = AgentProperty.getInstance().getPortMax();
		for(int i = portMin; i < portMax; i++) {
			createConnection(i);
			if (socket != null) {
				logger.debug("AgentFileSendTask connect to {}:{}", host, port);
				break;
			}
		}
		if (socket == null) {
			logger.error("AgentFileSendTask connection fail to {}:{}", host, port);
			disconnect();
		} else {
			is = socket.getInputStream();
			os = socket.getOutputStream();
		}
	}
	
	public void disconnect() throws IOException {
		if (socket != null) {
			socket.close();			
		}
	}
	
	@Override
	public void run() {
		try {
			connect();
			if (isSocketAlive()) {
				logger.debug("{} file send to {}", fileName, socket);
				Path rootPath = Paths.get(AgentProperty.getInstance().getFilePath(), fileName);
				File file = rootPath.toFile();
				if (file.exists() && file.isFile()) {
					DataOutputStream dos = new DataOutputStream(os);
					dos.writeUTF(fileName);
					FileInputStream fis = new FileInputStream(file);
					long fileSize = file.length();
					dos.writeLong(fileSize);
					sendFile(fis, dos);
					fis.close();
//					DataInputStream dis = new DataInputStream(is);
//					String result = dis.readUTF();
					
//					if ("success".equals(result)) {
//						agentController.success(result + ":" + fileName + " to " + socket.getInetAddress().getHostAddress());
//					}
				} else {
					//TODO  file not found response;
				}
			}
			
		} catch (IOException ioe) {
			logger.error("Failed to send a file");
		} finally {
			try {
				disconnect();
			} catch (IOException ignore) {}
		}
	}
	
	private boolean sendFile(InputStream in, OutputStream out) {
		int length;
		byte[] buff = new byte[2560];
		CipherOutputStream oStream = AESEncUtil.getEncryptStream(out);
		try {
			while((length = in.read(buff)) > 0) {
				oStream.write(buff, 0, length);
				oStream.flush();
			}
			oStream.close();
			return true;
		} catch (IOException e) {
			logger.error("fail to send a file", e);
			return false;
		}
	}
	
	public boolean isSocketAlive() {
		return socket.isConnected() && !socket.isClosed();
	}
}
