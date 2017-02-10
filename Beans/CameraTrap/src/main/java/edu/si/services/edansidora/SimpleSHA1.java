package edu.si.services.edansidora;
	import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
	import java.security.NoSuchAlgorithmException;
	import org.apache.cxf.common.util.Base64Utility;


	/**
	 * The Class SimpleSHA1.
	 */

	public class SimpleSHA1 {

		/**
		 * Convert to hex.
		 *
		 * @param data
		 *            the data
		 *
		 * @return the string
		 */
		private static String convertToHex(byte[] data) {
			StringBuffer buf = new StringBuffer();
			for (byte element : data) {
				int halfbyte = (element >>> 4) & 0x0F;
				int two_halfs = 0;
				do {
					if ((0 <= halfbyte) && (halfbyte <= 9)) {
						buf.append((char) ('0' + halfbyte));
					}
					else {
						buf.append((char) ('a' + (halfbyte - 10)));
					}
					halfbyte = element & 0x0F;
				} while (two_halfs++ < 1);
			}
			return buf.toString();
		}

		/**
		 * SHA1.
		 *
		 * @param text
		 *            the text
		 *
		 * @return the string
		 *
		 * @throws NoSuchAlgorithmException
		 *             the no such algorithm exception
		 * @throws UnsupportedEncodingException
		 *             the unsupported encoding exception
		 */
		public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
			MessageDigest md;
			md = MessageDigest.getInstance("SHA-1");
			byte[] sha1hash = new byte[40];
			md.update(text.getBytes("UTF-8"), 0, text.length());
			sha1hash = md.digest();

			// now convert to bash64
			String returnEncode = convertToHex(sha1hash);

			return returnEncode;
		}

		/**
		 * SH a1plus base64.
		 *
		 * @param text
		 *            the text
		 *
		 * @return the string
		 *
		 * @throws NoSuchAlgorithmException
		 *             the no such algorithm exception
		 * @throws UnsupportedEncodingException
		 *             the unsupported encoding exception
		 */
		public static String SHA1plusBase64(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {

			String sha1text = SHA1(text);
			byte[] b = sha1text.getBytes(Charset.forName("UTF-8"));
			String returnEncode = Base64Utility.encode(b);
			return returnEncode;
		}

}
