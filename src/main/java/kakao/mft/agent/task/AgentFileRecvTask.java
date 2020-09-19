package kakao.mft.agent.task;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import javax.crypto.CipherInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.control.AgentClient;
import kakao.mft.agent.control.ControllerStatus;
import kakao.mft.agent.props.AgentProperty;
import kakao.mft.agent.security.AESEncUtil;

public class AgentFileRecvTask implements Callable<Void> {
	
	private Logger logger = LoggerFactory.getLogger(AgentFileRecvTask.class);
	
	private AgentClient agentClient;
	private Socket socket;
	private ControllerStatus status;
	private InputStream is;
//	private OutputStream os;
	
	public AgentFileRecvTask(AgentClient agentClient, Socket socket, ControllerStatus status) {
		this.agentClient = agentClient;
		this.socket = socket;
		this.status = status;
		this.is = null;
	}
	
	public Void call() {
		try {
			is = socket.getInputStream();
//			os = socket.getOutputStream();
		} catch (IOException ioe) {
			logger.error("Failed to initailize socket stream", ioe);
			closeSocket();
		}
		
		if (is != null) {
			logger.debug("Agent file receiver connected from {}", socket.getRemoteSocketAddress());
			
			if (isSocketAlive()) {
				DataInputStream dis = new DataInputStream(is);
//				DataOutputStream dos = new DataOutputStream(os);
				
				File file = null;
				long fileSize = 0;
				String fileName = null;
				try {
					fileName = dis.readUTF();
//					logger.debug("--File Name:{}", fileName);
					Path rootPath = Paths.get(AgentProperty.getInstance().getFilePath());
					File rootDir = rootPath.toFile();
					
					if (!rootDir.exists()) {
						rootDir.mkdirs();
					}
					
					Path path = rootPath.resolve(fileName);
					file = path.toFile();
//					logger.debug("--File path:{}", file.getAbsoluteFile());
					fileSize = dis.readLong();
//					logger.debug("--File size:{}", fileSize);
				} catch (IOException ioe) {
					logger.error("Agent file receive exception", ioe);
				}
				
				if (dis != null) {
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(file);			
						byte[] buff = new byte[4096];
						int length;
						int saveFileSize = 0;
						CipherInputStream iStream = AESEncUtil.getDecryptStream(dis);
						try {
							while((length = iStream.read(buff)) > 0 && status.isRun()) {
								saveFileSize += length;
								fos.write(buff, 0, length);
							}
						} catch (IOException e) {
							logger.error("Agent file processing exception", e);
						}
						logger.debug("originFileSize: {}, saveFileSize: {}", fileSize, saveFileSize);
						
						agentClient.success("success:"+ fileName + " saved to " + agentClient.getAgentName());
//						dos.writeUTF("success");
					} catch (Exception e) {
						logger.error("Agent file receive stream exception", e);
					} finally {
						if (fos != null) {
							try {
								fos.flush();
								fos.close();								
							} catch (IOException ignore) {} 
						}
					}
				}
			}
		}
		return null;
	}
	
	public boolean isSocketAlive() {
		return socket.isConnected() && !socket.isClosed();
	}
	
	public void closeSocket() {
		if (!socket.isClosed()) {
			logger.debug("Closing socket of AgentFileRecvTask");
			try {				
				socket.close();			
			} catch (IOException ignore) {}
		}
	}
}
