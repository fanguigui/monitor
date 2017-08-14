package plugin.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderUtil {
	public static void addPluginURLIfAbsent(URLClassLoader classLoader) throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Method ADD_URL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		ADD_URL.setAccessible(true);
		final URL[] urls = classLoader.getURLs();
        if (urls != null) {
            ADD_URL.invoke(classLoader, ClassPathUtil.resolvePlugins()[0]);
        }
    }
}
