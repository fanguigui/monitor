# monitor
javagent+javassist实现的java监控工具，目前对dubbo实现了增强，用户可修改、自定义其他jar的增强

## 安装与使用
	* clone代码到本地，并转化为maven工程
	* 编译成功后执行`mvn clean install`命令，生成plugin.jar
	* 在target目录新建lib目录，并将依赖的javassist-3.21.0-GA.jar拷贝到lib目录
	* 修改应用tomcat的jvm参数，新增以下内容` -javaagent:D:\workspace\plugin\target\plugin.jar`，此处的`D:\workspace\plugin\target\plugin.jar`为第二步生成jar的绝对路径
	* 启动应用tomcat，可以发现dubbo服务消费时打印相关日志
	
## 说明
	本例参考了pinpoint的思路及部分代码，欢迎fork并修改，如有疑问可mailTo: fanguigui@meitunmama.com
