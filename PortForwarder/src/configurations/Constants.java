package configurations;

public class Constants {
	public static final String HOME_DIR = System.getProperty("user.home");
	public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	
	public static final int GATEWAY_PORT = 22;

	public static final int LOCAL_PORT = 51551;
	
	public static final String SERVICE_IP = "192.168.0.201";
	public static final int SERVICE_PORT = 3000;
	
	public static final String BLACKLIST_REGEX = "(.*(([^a-zA-Z0-9\\-\\ \\.!\\?ιθηΰµω\\_\\+\\-])|(null)|(sudo)).*)";
	
	public static String getPartialIPRegex() {
		String partialBlock = "(([01]?[0-9]{0,2})|(2[0-4][0-9])|(25[0-5]))";
        String subsequentPartialBlock = "(\\."+partialBlock+")";
        String ipAddress = partialBlock+"?"+subsequentPartialBlock+"{0,3}";
        return "^"+ipAddress ;
	}
}
