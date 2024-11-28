package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;

import java.util.Arrays;

import static burp.api.montoya.http.handler.ResponseReceivedAction.continueWith;


public class HttpResponseDecoder implements HttpHandler {
	
	private Logging logging;
	
	public HttpResponseDecoder(MontoyaApi api) {
		this.logging = api.logging();
	}
	
	@Override
	public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
		return null;
	}
	
	
	/**
	 * 处理接收的HTTPBody并将其中的unicode编码转换为中文
	 * @param httpResponseReceived 接收的HTTP请求
	 * @return ResponseReceivedAction 处理完的HTTP接收请求
	 */
	@Override
	public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
		String responseBody = httpResponseReceived.bodyToString();
		
		String asciicode = extractUnicodeCodes(responseBody);
		
		String httpResponseReceivedDecoderStr = null;
		
		if (!asciicode.isEmpty()) {
			logging.logToOutput("检测到unicode字符串,开始解码...");
			httpResponseReceivedDecoderStr = replaceUnicodeWithChars(responseBody, asciicode);
		}
		
		// 构造新的响应体
		byte[] newResponseBodyBytes = httpResponseReceivedDecoderStr != null ? httpResponseReceivedDecoderStr.getBytes() : responseBody.getBytes();
		HttpResponse newResponse = httpResponseReceived.withBody(Arrays.toString(newResponseBodyBytes));
		
		// 返回新的响应
		return continueWith(newResponse, httpResponseReceived.annotations());
	}
	
	/**
	 * 提取字符串中的所有Unicode编码
	 *
	 * @param input 输入的字符串
	 * @return 包含所有Unicode编码的字符串
	 */
	public static String extractUnicodeCodes(String input) {
		StringBuilder unicodeCodes = new StringBuilder();
		
		for (int i = 0; i < input.length(); ) {
			int codePoint = input.codePointAt(i);
			if (codePoint > 127) {
				// 如果是，则将其转换为Unicode编码格式
				String code = "\\u" + Integer.toHexString(codePoint).toUpperCase();
				unicodeCodes.append(code).append(" ");
			}
			i += Character.charCount(codePoint);
		}
		
		return unicodeCodes.toString().trim();
	}
	
	/**
	 * 将提取的Unicode编码转换回中文字符，并替换掉原字符串中的Unicode编码
	 *
	 * @param input        输入的字符串
	 * @param unicodeCodes 提取的Unicode编码字符串
	 * @return 替换后的字符串
	 */
	public static String replaceUnicodeWithChars(String input, String unicodeCodes) {
		String[] codes = unicodeCodes.split(" ");
		for (String code : codes) {
			if (!code.isEmpty()) {
				// 将Unicode编码转换回字符
				char[] chars = new char[2];
				int len = code.length();
				chars[0] = (char) Integer.parseInt(code.substring(2, len - 2), 16);
				chars[1] = (char) Integer.parseInt(code.substring(len - 2), 16);
				char ch = (char) Integer.parseInt(code.substring(2), 16);
				
				// 替换原字符串中的Unicode编码
				String unicodePattern = "\\\\u" + code.substring(2).toLowerCase();
				input = input.replace(unicodePattern, String.valueOf(ch));
			}
		}
		
		return input;
	}
}
