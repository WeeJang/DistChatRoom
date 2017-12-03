package edu.jangwee.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import edu.jangwee.component.ChatUserInstance;
import edu.jangwee.message.AckMessage;
import edu.jangwee.message.Message;
import edu.jangwee.message.MessageId;
import edu.jangwee.message.Protocal;

/**
 * 非组leader 持有的TCP处理者
 * 
 * @author Jang Wee
 *
 */
public class TCPClientProcessor implements Runnable, ITCPProcessor {
	// 宿主
	private Object owner;
	// Socket
	private Socket mSocket;
	// 线程控制
	private boolean isClientProcessorRun;
	
	public TCPClientProcessor(Object owner,String ipAddress) throws IOException {
		this.owner = owner;
		this.mSocket = new Socket();
		//关闭socket,立即释放底层资源
		mSocket.setSoLinger(true, 0);
		//端口重用
		mSocket.setReuseAddress(true);
		// 连接到leader 服务器
        mSocket.connect(new InetSocketAddress(ipAddress, Protocal.user_tcp_port));
		// 使能标志位
		this.isClientProcessorRun = true;
	}

	public void run() {
		//输入流对象
		ObjectInputStream in = null;	
		while (isClientProcessorRun) {
				try {
					in = new  ObjectInputStream(this.mSocket.getInputStream());
				} catch (IOException e) {
					e.printStackTrace();
					//探测到断开，进行处理
					handleBrokenLink();
				}
				
				//对象读取
				Object obj = null;
				try {
					obj = in.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(obj == null){
					continue;
				}
				if ((obj.getClass() == Message.class)
						|| (obj.getClass() == AckMessage.class)) {
					//处理消息
					handleMessage((Message) obj);
				}
				//线程暂停一会
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} 
				
			} 

	//处理链接断开
	public void handleBrokenLink(){
		//发起选举
		((ChatUserInstance) owner).startElection();
		// 设置消息类型 ： elect
		Message msg = new Message(Protocal.elect);
		// udp 发送消息
		((ChatUserInstance) owner).sendGroupTalkMessage(msg);
		// 退出该处理者
		quitProcessor();
		
	}
	
	//发送消息。所有经过Tcp发送的消息都由此送出
	public void sendMessage(Message msg) {
		try {
			// 设置消息ID
			MessageId msgId = new MessageId();		
			// 设置消息Id中的进程号
			msgId.setProcessId(((ChatUserInstance)owner).getProcessId());
			// 设置消息Id中的时间戳
			msgId.setTimeStamp(((ChatUserInstance)owner).getCurrentTime());
			// 将消息Id 附加到消息上
			msg.setMessageId(msgId);
			
			//发送出去
			ObjectOutputStream out = new ObjectOutputStream(this.mSocket.getOutputStream());
			out.writeObject(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 退出TCP处理者
	public void quitProcessor() {
		// 终止消息处理线程
		this.isClientProcessorRun = false;
		//关闭 socket
		if(mSocket != null){
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//处理收到的消息
	public void handleMessage(Message msg) {
		//控制台打印
		System.out.println("客户端TCP处理消息 ： "  + msg.toString());
		//更新时间
		updateCurrentTime(msg);
		//根据消息类型，进行处理
		switch (msg.getMessageType()) {
		//对请求端口号的回复
		case Protocal.request_process_id_ack:
			AckMessage amsg = (AckMessage) msg;
			// 设置该客户端的ProcessId
			((ChatUserInstance) this.owner).setProcessId(Long.valueOf((amsg
					.getMessageInfo())));
			break;

		default:
			break;
		}
	}
	
	// 更新当前时间
	private void updateCurrentTime(Message msg) {
		((ChatUserInstance) this.owner).updateTimeStamp(msg);
	}

	@Override
	public void initialize() {
		// 启动消息处理线程
		new Thread(this).start();
		// 向leader发送processId请求
		sendMessage(new Message(Protocal.request_process_id));
	}
}
