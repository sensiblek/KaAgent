package kakao.mft.agent.security;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

public class AESKeyspec {
	private Key keySpec;
	private String key;
	
	public AESKeyspec(String key) throws UnsupportedEncodingException {
		this.key = key.substring(0, 32);
		byte[] b = getIV();			
		byte[] keyBytes = new byte[16];
		int len = b.length;
		if (len > keyBytes.length) {
			len = keyBytes.length;
		}
		System.arraycopy(b, 0, keyBytes, 0, len);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		this.keySpec = keySpec;
	}

	public final Key getKeySpec() {
		return keySpec;
	}

	public final String getKey() {
		return key;
	}
	
	private static final String hexStringToString(String hex) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i < hex.length(); i=i+2) {
			String s = hex.substring(i, i+2);
			int n = Integer.valueOf(s, 16);
			builder.append((char)n);
		}
		return builder.toString();
	}
	
	public byte[] getIV() {
		return hexStringToString(key).getBytes(StandardCharsets.UTF_8);
	}
}
