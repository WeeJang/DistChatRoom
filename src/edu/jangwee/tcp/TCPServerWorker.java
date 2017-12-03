package edu.jangwee.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import edu.jangwee.component.ChatUserInstance;
import edu.jangwee.message.AckMessage;
import edu.jangwee.message.Message;
import edu.jangwee.message.MessageId;
import edu.jangwee.message.Protocal;

public class TCPServerWorker implements Runnable {
	// 对应的客户端的进程号
	private Long processId;
	// socket
	private Socket mSocket;
	// owner
	private Object owner;
	// client IpAddress
	private String ipAddress;
	// 自身线程控制
	private boolean isWorkerRun;
	
	public TCPServerWorker(Object owner, Long processId, 
			Socket socket) throws IOException {
		this.owner = owner;
		this.processId = processId;
		this.mSocket = socket;		
		this.isWorkerRun = true;
		this.ipAddress =  mSocket.getInetAddress().getHostAddress();
	}
	
	@Override
	public void run() {
		ObjectInputStream in = null;
				while (isWorkerRun) {
				try {
					in =  new ObjectInputStream(mSocket.getInputStream());
				} catch (IOException e) {
					e.printStackTrace();
					//处理断开的TCP链接
					handleBrokenLink();
				}
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
				// 检验得到的对象
				if ((obj.getClass() == Message.class)
						||  (obj.getClass() == AckMessage.class)) {
					handleMessge( (Message) obj);
				}
				// 让线程暂停一会
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
						e.printStackTrace();
				}			
			
		}
	}
	
	// 处理断开的TCP链接
	//因为这是leader 持有的，所以当检测到一个tcp断开时，应该向组内其他成员广播这个消息，告知他们有用户离开，删掉自己储存的这个用户的记录
	private void handleBrokenLink(){
		//控制台打印
		System.out.println("a user leader : " + ipAddress);
		// 发送UDP广播消息，告知伙伴，有人离开
		Message leaveMsg = new Message(Protocal.leave_client_info);
		leaveMsg.setMessageInfo(ipAddress);
		//udp发送消息
		sendUDPMessage(leaveMsg);
		//删掉该用户的信息
		removeUser(ipAddress);
		//退出该线程
		quitWorker();
	}
	// 消息处理
	private void handleMessge(Message msg) {
		//控制台打印
		System.out.println(" 服务端 Worker 处理消息  : " + msg.toString());
		//更新时间
		updateCurrentTime(msg);

		switch (msg.getMessageType()) {
		// 该消息为客户端请求分配进程号
		case Protocal.request_process_id:
			//组装回复消息
			AckMessage amsg = new AckMessage(Protocal.request_process_id_ack,
					msg.getMessageId());
			//设置消息的ID
			MessageId msgId = new MessageId();
			msgId.setProcessId(((ChatUserInstance) ((TCPServerProcessor) this.owner)
					.getOwner()).getProcessId());
			msgId.setTimeStamp(getCurrentTime());
			amsg.setMessageId(msgId);
			//设置消息内容为 分配的进程号
			amsg.setMessageInfo(processId.toString());
			// tcp发送消息
			sendMessage(amsg);
			break;

		default:
			break;
		}
	}
	// 退出Worker
	public void quitWorker() {
		//退出处理线程
		this.isWorkerRun = false;
		//关闭 socket
		if(mSocket != null){
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//发送TCP消息
	public void sendMessage(Message msg)  {	
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(mSocket.getOutputStream());
			out.writeObject(msg);
			Thread.sleep(200);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//获取当前时间
	private Long getCurrentTime() {
		return ((TCPServerProcessor) this.owner).getCurrentTime();
	}
	
	//更新时间
	private void updateCurrentTime(Message msg) {
		((TCPServerProcessor) this.owner).updateCurrentTime(msg);
	}
	// 发送UDP消息
	private void sendUDPMessage(Message msg){
		ChatUserInstance  userInstance =
				(ChatUserInstance)(((TCPServerProcessor) this.owner).getOwner());
		userInstance.sendGroupTalkMessage(msg);
	}
	//从同组用户记录中移除某IP用户的记录
	private void removeUser(String ipAddress){
		TCPServerProcessor serverProcessor = (TCPServerProcessor) this.owner;
		serverProcessor.removeClientNote(ipAddress);
		ChatUserInstance  userInstance =
				(ChatUserInstance)(serverProcessor.getOwner());
		userInstance.removeGroupUser(ipAddress);	
	}
}
