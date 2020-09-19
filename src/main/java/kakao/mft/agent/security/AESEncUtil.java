package kakao.mft.agent.security;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kakao.mft.agent.props.AgentProperty;

public class AESEncUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(AESEncUtil.class);
	
	private static final String AES = "AES/CBC/PKCS5Padding";
	private static AESKeyspec keySpec;
	
	public static CipherInputStream getDecryptStream(InputStream inStream) {
		keyInit();
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(AES);
			cipher.init(Cipher.DECRYPT_MODE, keySpec.getKeySpec(), new IvParameterSpec(keySpec.getIV()));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			logger.error("Failed to get decryption stream", e);
		}
		return new CipherInputStream(inStream, cipher);
	}
	
	public static CipherOutputStream getEncryptStream(OutputStream stream) {
		keyInit();
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(AES);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec.getKeySpec(), new IvParameterSpec(keySpec.getIV()));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			logger.error("Failed to get encryption stream", e);
		}
		return new CipherOutputStream(stream, cipher);
	}
	
	private static synchronized void keyInit() {
		try {
			if (keySpec == null) {
				keySpec = new AESKeyspec(AgentProperty.getInstance().getAesKey());
			}
		} catch (Exception e) {
			logger.error("AES key initialization failed", e);
		}
	}
}
