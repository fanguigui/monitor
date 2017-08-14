package plugin.interceptor;

public interface Interceptor {
	public void before(Object invoker, Object param);
	
	public void after(Object invoker, Object param, Object result, Throwable throwable);
}
