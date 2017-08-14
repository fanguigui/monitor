package plugin.util;

import java.util.HashMap;
import java.util.Map;

public class InjectCacheUtil {
	private static final Map<String, String> map = new HashMap<String, String>();
	
	public static void put(String key, String value){
		map.put(key, value);
	}
	
	public static String get(String key){
		return map.get(key);
	}
}
