public class ServerInfo {

	private static ServerInfo instance = null;
	private String machineURL  = "http://info.deepcarbon.net/vivo";
	private String absoluteMachineURL = "http://info.deepcarbon.net/vivo";
	private String handleURL = "http://128.213.3.13:8080/dcohandleservice/services/handles/";
	private String ckanURL = "http://data.deepcarbon.net/ckan";
	private String dcoOntoNameSpace = "http://info.deepcarbon.net/schema#";
	private String dcoNamespace = "http://info.deepcarbon.net";
	private String rootName = "XXXXX";
	private String rootPassword = "XXXXXX";
	
	private void ServerInfo(){
		machineURL = "http://udco.tw.rpi.edu/vivo";
		absoluteMachineURL = "http://udco.tw.rpi.edu/vivo";
		handleURL = "http://128.213.3.13:8080/dcohandleservice/services/handles/";
		ckanURL = "http://data.deepcarbon.net/ckan";
		dcoOntoNameSpace = "http://info.deepcarbon.net/schema#";
		dcoNamespace = "http://info.deepcarbon.net";
		rootName = "XXXXX";
		rootPassword = "XXXX";
		
	}
	
	public static ServerInfo getInstance(){
		if(instance==null){
			return new ServerInfo();
		}else{
			return instance;
		}
	}
	public String getRootName(){
		return this.rootName;
	}
	
	public String getRootPassword(){
		return this.rootPassword;
	}
	
	public String getMachineURL(){
		return this.machineURL;
	}
	
	public String getHandleURL(){
		return this.handleURL;
	}
	
	public String getCkanURL(){
		return this.ckanURL;
	}
	
	public String getDcoOntoNamespace(){
		return this.dcoOntoNameSpace;
	}
	
	public String getDcoNamespace(){
		return this.dcoNamespace;
	}
	public String getAbsoluteMachineURL(){
		return this.absoluteMachineURL;
	}
}
