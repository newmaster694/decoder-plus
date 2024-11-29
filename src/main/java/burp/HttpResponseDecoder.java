package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	 *
	 * @param httpResponseReceived 接收的HTTP请求
	 * @return ResponseReceivedAction 处理完的HTTP接收请求
	 */
	@Override
	public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
		String responseBody = httpResponseReceived.bodyToString();
		
		List<String> unicodes = extractUnicodeCodes(responseBody);
		
		String httpResponseReceivedDecoderStr = null;
		
		if (StrUtil.isNotBlank(unicodes.toString())) {
			logging.logToOutput("检测到unicode字符串,开始解码...");
			httpResponseReceivedDecoderStr = replaceUnicodeWithChars(responseBody, unicodes);
		}
		
		// 构造新的响应体
		byte[] newResponseBodyBytes = httpResponseReceivedDecoderStr != null ? httpResponseReceivedDecoderStr.getBytes(StandardCharsets.UTF_8) : responseBody.getBytes(StandardCharsets.UTF_8);
		HttpResponse newResponse = httpResponseReceived.withBody(ByteArray.byteArray(newResponseBodyBytes));
		
		// 返回新的响应
		return continueWith(newResponse, httpResponseReceived.annotations());
	}
	
	/**
	 * 提取字符串中的所有Unicode编码
	 *
	 * @param input 输入的字符串
	 * @return 包含所有Unicode编码的字符串
	 */
	public static List<String> extractUnicodeCodes(String input) {
			// 定义匹配Unicode编码的正则表达式
			Pattern pattern = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
			Matcher matcher = pattern.matcher(input);
			
			// 创建一个列表来保存所有的Unicode编码
			List<String> unicodeCodes = new ArrayList<>();
			
			// 查找所有匹配项并添加到列表中
			while (matcher.find()) {
				unicodeCodes.add(matcher.group());
			}
			
			return unicodeCodes;
	}
	
	/**
	 * 将提取的Unicode编码转换回中文字符，并替换掉原字符串中的Unicode编码
	 * @param input        输入的字符串
	 * @param unicodeCodes 提取的Unicode编码字符串
	 * @return 替换后的字符串
	 */
	public static String replaceUnicodeWithChars(String input, List<String> unicodeCodes) {
		for (String unicode : unicodeCodes) {
			if (StrUtil.isNotBlank(unicode)) {
				String res = UnicodeUtil.toString(unicode);
				
				input = input.replace(unicode, res);
			}
		}
		
		return input;
	}
}
