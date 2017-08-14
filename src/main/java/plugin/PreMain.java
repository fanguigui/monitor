package plugin;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

import plugin.instrument.ClassFileInstrument;
import plugin.util.ClassPathUtil;

public class PreMain {
	public static void premain(String options, Instrumentation ins) {
        //注册我自己的字节码转换器
		JarFile jarFile;
		String classPath = System.getProperty("java.class.path");
		String agentPath = ClassPathUtil.parseAgentJarPath(classPath, "plugin.jar");
		String agentDir = ClassPathUtil.parseAgentDirPath(agentPath);
		ClassPathUtil.put("dir", agentPath);
		try {
			//此处添加以找到javassist的类
			jarFile = new JarFile(agentDir + "\\lib\\javassist-3.21.0-GA.jar");
			ins.appendToBootstrapClassLoaderSearch(jarFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
        ins.addTransformer(new ClassFileInstrument());
    }
}
