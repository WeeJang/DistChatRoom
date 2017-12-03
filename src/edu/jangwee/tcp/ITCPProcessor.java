package edu.jangwee.tcp;
/**
 * @author Jang Wee
 * 
 *  定义了一个接口，TCP 处理
 */

import edu.jangwee.message.Message;

public interface ITCPProcessor {
	// 初始化，开启
	public void initialize();
	// 发送消息
	public void sendMessage(Message msg);
	// 退出
	public void quitProcessor();
	// 处理消息
	public void handleMessage(Message msg);
}
