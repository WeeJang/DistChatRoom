package edu.jangwee.component;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;

import edu.jangwee.message.Message;
import edu.jangwee.message.MessageId;
import edu.jangwee.message.Protocal;
import edu.jangwee.order.OrderQueueHandler;
import edu.jangwee.tcp.TCPClientProcessor;
import edu.jangwee.tcp.ITCPProcessor;
import edu.jangwee.tcp.TCPServerProcessor;
import edu.jangwee.udp.UDPProcessor;

/**
 * 当前用户聊天实例
 * 
 * @author Jang Wee
 *
 */
public class ChatUserInstance {
	
	// 宿主，持有该实例的对象
	private Object owner;
	// Lamport Logic Clock 时间戳
	private long lamportLogicClock;
	// 当前实例进程的Id
	private long processId;
	// 当前加入或者创建的组
	private ChatGroup currentChatGroup;
	//用户名
	private String userName;
	// 组端口号
	private int groupPort;
	// 是否是组leader
	private boolean isLeader;
	//是否total order
	private boolean isTotalOrder;

	// 选举相关的一些状态位
	// 是否处于选举阶段
	private boolean election;
	// 是否已经在选举中失败
	private boolean isLoser;
	// 记录发送elect的时间
	private long electionTime;

	// 当前会话组所有用户信息记录表<IpAddress,ProcessId>
	private HashMap<String, Long> groupUserInfoMap;

	// TCP 通信处理者：
	// leader 类是实例化 TCPServerProcessor ,非leader类是实例化 TCPClientProcessor
	private ITCPProcessor mTcpProcessor;

	//UDP 通信处理者
	// leader 非leader 相同
	private UDPProcessor mUdpProcessor;
	
	//资源申请【消息显示】排队处理者 
	private OrderQueueHandler mOrderQueueHandler;
	

	public ChatUserInstance(Object owner) {
		ChatRoomFrame chatRoomFrame = (ChatRoomFrame)owner;
		this.owner = owner;
		this.userName = chatRoomFrame.getUserName();
		this.isLeader = chatRoomFrame.isLeader();
		this.isTotalOrder = chatRoomFrame.isTotalOrder();
		this.groupPort = chatRoomFrame.getGroupPort();
		this.currentChatGroup = chatRoomFrame.getCurrentGroup();
		
		this.groupUserInfoMap = new HashMap<String, Long>();
		// 如果是创建者，则默认processId 为 3;否则 为 99999  请求leader分配
		this.processId = (isLeader ? 3 : 99999); // 1 为release_chat  2  : release_ack 
		//时间戳从10 开始
		//这是因为为了便于将一些把一些高等级消息如release消息在排队中靠前，
		//会把这些消息设置成0 1 等比10 小的数字
		this.lamportLogicClock = 10;
		
		initialize();
	}

	// 初始化
	private void initialize() {
		// 初始化 mTcpProcessor
		// 是否是leader
		if (isLeader) {
			// 创建TCP服务端处理者
			mTcpProcessor = new TCPServerProcessor(this);
		} else {
			// 创建TCP客户端处理者
			try {	
				mTcpProcessor = new TCPClientProcessor(this,
						currentChatGroup.getLeaderIp());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//初始化 mUdpProcessor 
		// UDP Processor
		try {
			mUdpProcessor = new UDPProcessor(ChatUserInstance.this,
					currentChatGroup.getGroupPort());
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//初始化消息队列处理者
		mOrderQueueHandler = new OrderQueueHandler(this, isTotalOrder);
		
		//处理者内部初始化
		mTcpProcessor.initialize();
		mUdpProcessor.initialize();
		mOrderQueueHandler.initiliaze();
		
		
		//如果自己是新加入的成员，需要广播 request_init_info 请求，以获取其他组内成员信息
		if( !isLeader ){
			//设置消息类型 ： request_init_info
			Message msg = new Message(Protocal.request_init_info);
			//设置消息内容 ： 自己的用户名 
			msg.setMessageInfo(this.userName);
			// udp 发送消息
			mUdpProcessor.sendMessage(msg);
		}

	}
	
	// 主动更新时钟
	public  void updateTimeStamp() {
		synchronized (this) {
			// 自增
			this.lamportLogicClock++;
			// UI 显示更新时钟
			((ChatRoomFrame)owner).displayClock(lamportLogicClock);
		}
	}

	// 根据收到的消息，被动更新时钟
	public void updateTimeStamp(Message msg) {
		synchronized (this) {
			// 判断消息是否有有效的Id
			if(msg.getMessageId() != null){
				//判断消息时间是否大于当前时钟
				if (msg.getMessageId().getTimeStamp() > this.lamportLogicClock) {
					// 将自己的时钟更新到该消息的时间戳
					this.lamportLogicClock = msg.getMessageId().getTimeStamp();
				}
			}
			// 主动更新时钟
			updateTimeStamp();
		}
	}
	// 返回当前时间
	public long getCurrentTime() {
		synchronized (this) {
			return lamportLogicClock;
		}
	}

	// 释放实例，清理资源
	public void freeInstance() {
		// TCP,UDP,OrderQueue 处理者退出
		mTcpProcessor.quitProcessor();
		mUdpProcessor.quitProcessor();
		mOrderQueueHandler.stopOrderQueueHandler();
	}


	// 选举监听器 
	// 当探测到与Leader的TCP链接断开时，会发送开始选举：elect  消息，并激活此监听器 , 处理各种选举过程中的消息
	// 组内其他成员 收到 elect 消息后，也会主动激活此监听器，处理各种选举过程中的消息
	// Bully 算法。如果收到其他比自己进程号大的 elect_ack ，则说明自己在此次选举中，已经失败，会设置 isLoser = true ，此监听器退出。等待选举过程
	private Runnable ElectionListener = new Runnable() {
		@Override
		public void run() {
			// 获取当前系统时间
			electionTime = System.currentTimeMillis();
			//  判断自己是否失去竞选资格
			while (!isLoser) {
				try {
					//判断等待选举消息是否超时
					if ((System.currentTimeMillis() - electionTime) > Protocal.elect_time_out) {
						// 超时，说明自己胜出
						
						//状态复位
						//选举结束
						election = false;
						//不是失败者
						isLoser = false;
						//成为leader
						isLeader = true;
						// 把当前组信息的leaderIp设置成自己的IP
						currentChatGroup.setLeaderIp(((ChatRoomFrame)owner).getLocalIpAddress());
		
						currentChatGroup.setLeaderPort(Protocal.user_tcp_port);
						((ChatRoomFrame)owner).getCurrentGroup()
												.setLeaderPort(Protocal.user_tcp_port);
						
						//更新这个
						TCPServerProcessor.idCreator = getProcessId();
						
						afterElection();	
						Message msg = new Message(Protocal.elect_finish);
						msg.getMessageId().setProcessId(getProcessId());
						msg.getMessageId().setTimeStamp(getCurrentTime());
						// 选举胜利者将 自己的 IP#Port广播出去，便于其他建立链接
						msg.setMessageInfo(	((ChatRoomFrame)owner).getLocalIpAddress()
								+ "#" + Protocal.user_tcp_port);
						System.out.println("Winner : + " +  msg);
						mUdpProcessor.sendMessage(msg);
						
						if(!(election) || isLoser)
							break;
					}
					Thread.sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	//启动选举
	public void startElection() {
		setElection(true);
		System.out.println("election start...");
		removeGroupUser(currentChatGroup.getLeaderIp());
		//TcP 退出
		mTcpProcessor.quitProcessor();
		mTcpProcessor = null ;
		
		new Thread(ElectionListener).start();
		
	}
	
	//election finish之后调用
	//此时 已经明确新的leader的ip,port
	public void afterElection(){	
		if (isLeader) {
			mTcpProcessor = new TCPServerProcessor(this);
		} else {
			try {
				mTcpProcessor = new TCPClientProcessor(this, currentChatGroup
						.getLeaderIp());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mTcpProcessor.initialize();
		updateState();
	}

	// 发送UDP消息[]
	public void sendGroupTalkMessage(Message msg) {
		MessageId msgId = new MessageId();
		msgId.setProcessId(getProcessId());
		 if(msg.getMessageType() == Protocal.release_chat){
			//最高优先级
			msgId.setTimeStamp(1);
			msg.setMessageId(msgId);
			addMessageIntoQueue(msg);
		}else if (msg.getMessageType() == Protocal.request_chat){
			// 不是重发
			 if(msg.getMessageId().getTimeStamp() == 0){
				 msgId.setTimeStamp(getCurrentTime());
					msg.setMessageId(msgId);
			 }
			 msg.setMessageOwner(userName);
			addMessageIntoQueue(msg);
		}else {
			msgId.setTimeStamp(getCurrentTime());
			msg.setMessageId(msgId);
		}
//		updateTimeStamp(); 
		
		mUdpProcessor.sendMessage(msg);
	}
	
	//提供插入消息接口
	public void addMessageIntoQueue(Message msg){
		this.mOrderQueueHandler.addMessage(msg);
		}
	
	//显示
	public void updateUI(Message msg)
	{
		((ChatRoomFrame)this.owner).displayMessage(msg);
	}
	
	//
	public void updateState(){
		((ChatRoomFrame)this.owner).setUserName(this.userName);;
		((ChatRoomFrame)this.owner).setCurrentGroup(this.currentChatGroup);
		((ChatRoomFrame)this.owner).setLeader(this.isLeader); 
		((ChatRoomFrame)this.owner).refreshStatus();
	}
	
	
	// 以下为成员变量的 Set or Get 方法
	
	public boolean isLeader() {
		return isLeader;
	}
	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
		((ChatRoomFrame)this.owner).setLeader(isLeader);
	}

	public long getElectionTime() {
		return electionTime;
	}

	public void setElectionTime(long electionTime) {
		this.electionTime = electionTime;
	}

	public boolean isElection() {
		return election;
	}

	public void setElection(boolean election) {
		this.election = election;
	}

	public boolean isLoser() {
		return isLoser;
	}

	public void setLoser(boolean isLoser) {
		this.isLoser = isLoser;
	}

	public String getUserName() {
		return this.userName;
	}

	public void setCurrentChatGroup(ChatGroup chatGroup) {
		this.currentChatGroup = chatGroup;
	}

	public ChatGroup getCurrentChatGroup() {
		return this.currentChatGroup;
	}

	public void addGroupUser(String ipAddress, Long processId) {
		this.groupUserInfoMap.put(ipAddress, processId);
	}

	public void removeGroupUser(String ipAddress) {
		this.groupUserInfoMap.remove(ipAddress);
	}

	public void cleanGroupUser() {
		this.groupUserInfoMap.clear();
	}

	public HashMap<String, Long> getGroupUser() {
		return this.groupUserInfoMap;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}

	public Long getProcessId() {
		return this.processId;
	}

	public Object getOwner() {
		return owner;
	}

	public void setOwner(Object owner) {
		this.owner = owner;
	}

	public int getGroupPort() {
		return groupPort;
	}

	public void setGroupPort(int groupPort) {
		this.groupPort = groupPort;
	}
	
	
}
