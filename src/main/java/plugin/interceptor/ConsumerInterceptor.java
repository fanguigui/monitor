package plugin.interceptor;

import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.fastjson.JSON;

public class ConsumerInterceptor implements Interceptor{
	
	public void before(Object invoker, Object param) {
		RpcInvocation invocation = (RpcInvocation) param;
		System.out.println("-------------------before execute----------------------");
		System.out.println("methodName:" + invocation.getMethodName() + ", params:" + JSON.toJSONString(invocation.getArguments()));
		invocation.setAttachment("key", "value");
	}

	public void after(Object invoker, Object param, Object result, Throwable throwable) {
		RpcInvocation invocation = (RpcInvocation) param;
		if(result != null){
			Result r = (Result)result;
			System.out.println("-------------------after execute----------------------");
			System.out.println("methodName:" + invocation.getMethodName() + ", params:" + JSON.toJSONString(invocation.getArguments()));
			System.out.println("result:" + JSON.toJSONString(r.getValue()));
			System.out.println("attachment:" + invocation.getAttachment("key"));
		}
		if(throwable != null){
			System.out.println("-------------------catch exception----------------------");
			throwable.printStackTrace();
		}
	}

}
