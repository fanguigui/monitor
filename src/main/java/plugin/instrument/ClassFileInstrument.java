package plugin.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import plugin.interceptor.ConsumerInterceptor;
import plugin.util.ClassLoaderUtil;
import plugin.util.InjectCacheUtil;

public class ClassFileInstrument implements ClassFileTransformer{
	private ClassPool defaultClassPool = ClassPool.getDefault();
	
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (className != null && className.indexOf("/") != -1) {
            className = className.replaceAll("/", ".");
        }
		if(className.equals("com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker")){
			//判断是否已经增强过
			if(InjectCacheUtil.get(className) != null){
				return null;
			}
			try{
				//将此plugin.jar加入到应用的classloader
				ClassLoaderUtil.addPluginURLIfAbsent((URLClassLoader)loader);
				final ClassPath classPath = new LoaderClassPath(loader);
				final ClassPool contextCassPool = new ClassPool();
				final ClassPath byteArrayClassPath = new ByteArrayClassPath(className, classfileBuffer);
		        contextCassPool.insertClassPath(byteArrayClassPath);
		        contextCassPool.appendClassPath(classPath);
		        CtClass ctClass = contextCassPool.get(className);
				byte [] code = generate(ctClass, "invoke", ConsumerInterceptor.class.getName());
				InjectCacheUtil.put(className, "1");
				return code;
			}catch(Exception e){
				e.printStackTrace();
			}
		}else if(className.equals("com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker")){
			//TODO:
//			try{
//				return generate(loader, className, "invoke", ConsumerInterceptor.class.getName());
//			}catch(Exception e){
//				e.printStackTrace();
//			}
		}
		return null;
	}

	public byte[] generate(CtClass ctClass, String method, String interceptorImplClassName){
		try{
			CtBehavior behavior = ctClass.getDeclaredMethod(method);
			boolean localVarsInitialized = false;
			int offset = insertBefore(behavior, interceptorImplClassName);
			
			if(offset != -1){
				localVarsInitialized = true;
			}
			
			
			insertCatchException(behavior, offset, localVarsInitialized, interceptorImplClassName);
			generateAfter(behavior, localVarsInitialized, interceptorImplClassName);
			
			return ctClass.toBytecode();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int insertBefore(CtBehavior behavior, String interceptorImplClassName) {
		try{
			CtClass interceptorClass = defaultClassPool.get("plugin.interceptor.Interceptor");
	        behavior.addLocalVariable("fantasy_plugin_instance", interceptorClass);
			int offset = insertBeforeMethod(behavior, -1, "fantasy_plugin_instance = null;");
			String code = "{try { fantasy_plugin_instance = new " + interceptorImplClassName + "(); fantasy_plugin_instance.before(this, ($w)$1);} catch (Exception e) { e.printStackTrace(); }}";
			return insertBeforeMethod(behavior, offset, code);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private void insertCatchException(CtBehavior behavior, int offset, boolean localVarsInitialized, String interceptorImplClassName) {
		try{
			StringBuilder codeBuilder = new StringBuilder("{try { ");
			if (!localVarsInitialized) {
				codeBuilder.append("fantasy_plugin_instance = null; ");
			 }
			codeBuilder.append("fantasy_plugin_instance = new " + interceptorImplClassName + "(); fantasy_plugin_instance.after(this, ($w)$1, null, $e);} catch (plugin.exception.PluginException e) { e.printStackTrace(); } throw $e;}");
			CtClass throwable = behavior.getDeclaringClass().getClassPool().get("java.lang.Throwable");
			insertCatch(behavior, offset, codeBuilder.toString(), throwable, "$e");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void generateAfter(CtBehavior behavior, boolean localVarsInitialized, String interceptorImplClassName) {
		try{
			StringBuilder codeBuilder = new StringBuilder("{try { ");
			if (!localVarsInitialized) {
				codeBuilder.append("fantasy_plugin_instance = null; ");
			 }
			codeBuilder.append("fantasy_plugin_instance = new " + interceptorImplClassName + "(); fantasy_plugin_instance.after(this, ($w)$1, ($w)$_, null);} catch (plugin.exception.PluginException e) { e.printStackTrace(); }}");
			behavior.insertAfter(codeBuilder.toString());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int insertBeforeMethod(CtBehavior behavior, int pos, String src) throws CannotCompileException {
        CtClass cc = behavior.getDeclaringClass();
        CodeAttribute ca = behavior.getMethodInfo().getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(cc);
        try {
            int nvars = jv.recordParams(behavior.getParameterTypes(), Modifier.isStatic(behavior.getModifiers()));
            jv.recordParamNames(ca, nvars);
            jv.recordLocalVariables(ca, 0);
            jv.recordType(Descriptor.getReturnType(behavior.getMethodInfo().getDescriptor(), behavior.getDeclaringClass().getClassPool()));
            jv.compileStmnt(src);
            Bytecode b = jv.getBytecode();
            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            if (locals > ca.getMaxLocals())
                ca.setMaxLocals(locals);
            
            if (pos != -1) { 
                iterator.insertEx(pos, b.get());
            } else {
                pos = iterator.insertEx(b.get());
            }
            
            iterator.insert(b.getExceptionTable(), pos);
            behavior.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
            
            return pos + b.length();
        } catch (NotFoundException e) {
            throw new CannotCompileException(e);
        } catch (CompileError e) {
            throw new CannotCompileException(e);
        } catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
	
	private void insertCatch(CtBehavior behavior, int from, String src, CtClass exceptionType, String exceptionName) throws CannotCompileException {
        CtClass cc = behavior.getDeclaringClass();
        ConstPool cp = behavior.getMethodInfo().getConstPool();
        CodeAttribute ca = behavior.getMethodInfo().getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(cp, ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(1);
        Javac jv = new Javac(b, cc);
        try {
            jv.recordParams(behavior.getParameterTypes(), Modifier.isStatic(behavior.getModifiers()));
            jv.recordLocalVariables(ca, from);
            int var = jv.recordVariable(exceptionType, exceptionName);
            b.addAstore(var);
            jv.compileStmnt(src);

            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            if (locals > ca.getMaxLocals())
                ca.setMaxLocals(locals);

            int len = iterator.getCodeLength();
            int pos = iterator.append(b.get());

            ca.getExceptionTable().add(from, len, len, cp.addClassInfo(exceptionType));
            iterator.append(b.getExceptionTable(), pos);
            behavior.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        } catch (NotFoundException e) {
            throw new CannotCompileException(e);
        } catch (CompileError e) {
            throw new CannotCompileException(e);
        } catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
}
