package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class BurpExtender implements BurpExtension {
	@Override
	public void initialize(MontoyaApi api) {
		api.extension().setName("HTTP response decoder");
		
		//将插件注册到这个"主类"中
		api.http().registerHttpHandler(new HttpResponseDecoder(api));
	}
}
