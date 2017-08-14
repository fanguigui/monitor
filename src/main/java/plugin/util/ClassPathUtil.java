package plugin.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ClassPathUtil {
	private static final Map<String, String> map = new HashMap<String, String>();
	
	public static void put(String key, String value){
		map.put(key, value);
	}
	
	public static String getDir(String key){
		return map.get(key);
	}
	
	public static URL[] resolvePlugins() {
        final File file = new File(getPluginPath());
        
        if (!file.exists()) {
            return new URL[0];
        }
        
        if (file.isDirectory()) {
            return new URL[0];
        }
        
        final URL[] urls = new URL[1];
        
        
        try {
            urls[0] = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Fail to load plugin jars", e);
        }

        return urls;
    }
	
	public static String parseAgentDirPath(String agentJarFullPath) {
        int index1 = agentJarFullPath.lastIndexOf("/");
        int index2 = agentJarFullPath.lastIndexOf("\\");
        int max = Math.max(index1, index2);
        if (max == -1) {
            return null;
        }
        return agentJarFullPath.substring(0, max);
    }
	
	public static String parseAgentJarPath(String classPath, String agentJar) {
        String[] classPathList = classPath.split(File.pathSeparator);
        for (String findPath : classPathList) {
            boolean find = findPath.contains(agentJar);
            if (find) {
                return findPath;
            }
        }
        return null;
    }
	
	public static String getPluginPath() {
        return map.get("dir");
    }
}
