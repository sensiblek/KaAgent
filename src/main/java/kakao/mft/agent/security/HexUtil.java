package kakao.mft.agent.security;

import kakao.mft.agent.constant.AgentConst;

public class HexUtil {
	public static final String bytesToHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length*2];
		for (int i=0; i < bytes.length; i++) {
			int v = bytes[i] & 0XFF;
			hexChars[i*2] = AgentConst.HEX_ARRAY[v >>> 4];
			hexChars[i*2+1] = AgentConst.HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}
