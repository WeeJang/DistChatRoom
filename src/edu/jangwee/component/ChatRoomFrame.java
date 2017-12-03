package edu.jangwee.component;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import edu.jangwee.message.Message;
import edu.jangwee.message.Protocal;

/**
 * @author Jang Wee
 *    UI 
 */
public class ChatRoomFrame {
	/**
	 * Lauch the application
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ChatRoomFrame window = new ChatRoomFrame();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//应用层UDP Socket,用于发现存在的会话组
	private static DatagramSocket applicationSocket;
	
	//UI组件	
	//主框架
	private JFrame frame;
	private JPanel mainPanel;
	//消息展示区域
	private JPanel showPanel;
	private JScrollPane showScrollPane;
	private JList<String> msgList;
	public DefaultListModel<String> msgListModel;
	//按钮功能区域 
	private JPanel buttonPanel;
	private JButton createButton;
	private JButton joinButton;
	private JButton leaveButton;
	//消息输入区域
	private JPanel userPanel;
	private JScrollPane writeScrollPane;
	private JTextArea msgWritedTextArea;
	private JButton cancelButton;
	private JButton sendButton;
	//	用户状态区域
	private JLabel userNameLabel;
	private JLabel isLeaderLabel;
	private JLabel isTotalOrderLabel;
	private JLabel currentGroupLabel;
	private JLabel isTalkingLabel;
	private JLabel lamportLogicClockLabel;
	private long clock;
	
	//当前用户信息
	//用户名
	private String userName;
	//组播端口
	private int groupPort;
	//是否是leader
	private boolean isLeader;
	//是否total order
	private boolean  isTotalOrder;
	// 当前IP
	private String localIpAddress;
	
	//当前是否已经加入会话
	private boolean isTalking;
	//当前用户实例
	private ChatUserInstance mChatUserInstance;
	//当前加入的会话组
	private  ChatGroup currentGroup;
	//已经存在的会话组<port,chatGroup>
	private HashMap<Integer, ChatGroup> chatGroupMap;


	//启动全局socket
	static{
		try {
			applicationSocket = new DatagramSocket(Protocal.app_global_upd_port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//构造函数
	public ChatRoomFrame() {
		initialize();
	}
	
	//初始化
	private void initialize() {
		
		// UI布局，加载
		frame = new JFrame();
		frame.setBounds(100, 100, 620, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainPanel = (JPanel) frame.getContentPane();
		mainPanel.setLayout(null);

		showPanel = new JPanel();
		showPanel.setBounds(5, 5, 400, 200);
		showPanel.setBorder(new TitledBorder(null, "Message",
				TitledBorder.LEADING, TitledBorder.TOP));
		showPanel.setForeground(Color.WHITE);
		showPanel.setLayout(null);

		showScrollPane = new JScrollPane();
		showScrollPane.setBounds(10, 15, 380, 180);
		showPanel.add(showScrollPane);
		msgListModel = new DefaultListModel<String>();
		msgList = new JList<>(msgListModel);
		msgList.setBackground(Color.WHITE);
		showScrollPane.setViewportView(msgList);
		
		mainPanel.add(showPanel);
		
		buttonPanel = new JPanel();
		buttonPanel.setForeground(Color.WHITE);
		buttonPanel.setBorder(new TitledBorder(null, "Button",
				TitledBorder.LEADING, TitledBorder.TOP));
		buttonPanel.setBounds(410, 5, 200, 200);
		buttonPanel.setLayout(null);

		createButton = new JButton();
		createButton.setBounds(20, 20, 150, 30);
		createButton.setText("Create Group");
		createButton.setEnabled(true);
		createButton.addActionListener(createGroupListener);
		buttonPanel.add(createButton);

		joinButton = new JButton();
		joinButton.setBounds(20, 55, 150, 30);
		joinButton.setText("Join Group");
		joinButton.setEnabled(true);
		joinButton.addActionListener(joinGroupListener);
		buttonPanel.add(joinButton);

		leaveButton = new JButton();
		leaveButton.setBounds(20, 90, 150, 30);
		leaveButton.setText("Leave Group");
		leaveButton.setEnabled(false);
		leaveButton.addActionListener(leaveGroupListener);
		buttonPanel.add(leaveButton);

		mainPanel.add(buttonPanel);

		userPanel = new JPanel();
		userPanel.setForeground(Color.WHITE);
		userPanel.setBorder(new TitledBorder(null, "User",
				TitledBorder.LEADING, TitledBorder.TOP));
		userPanel.setBounds(5, 210, 600, 200);
		userPanel.setLayout(null);

		writeScrollPane = new JScrollPane();
		writeScrollPane.setBounds(10, 15, 200, 100);
		userPanel.add(writeScrollPane);

		msgWritedTextArea = new JTextArea();
		writeScrollPane.setViewportView(msgWritedTextArea);

		sendButton = new JButton();
		sendButton.setBounds(10, 120, 90, 30);
		sendButton.setText("Send");
		sendButton.addActionListener(sendMessageListener);
		userPanel.add(sendButton);

		cancelButton = new JButton();
		cancelButton.setBounds(105, 120, 90, 30);
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(cancelMessageListener);
		userPanel.add(cancelButton);

		userNameLabel = new JLabel();
		userNameLabel.setBounds(240, 15, 250, 20);
		userNameLabel.setText("UserName : " + this.userName);
		userPanel.add(userNameLabel);

		isLeaderLabel = new JLabel();
		isLeaderLabel.setBounds(240, 40, 250, 20);
		isLeaderLabel.setText("isLeader : " + isLeader);
		userPanel.add(isLeaderLabel);

		isTotalOrderLabel = new JLabel();
		isTotalOrderLabel.setBounds(240, 65, 250, 20);
		isTotalOrderLabel.setText("isTotalOrder : " + isLeader);
		userPanel.add(isTotalOrderLabel);

		currentGroupLabel = new JLabel();
		currentGroupLabel.setBounds(240, 90, 250, 20);
		currentGroupLabel.setText("group : " + null);
		userPanel.add(currentGroupLabel);

		isTalkingLabel = new JLabel();
		isTalkingLabel.setBounds(240, 115, 250, 20);
		isTalkingLabel.setText("isTalking : " + isTalking);
		userPanel.add(isTalkingLabel);
		
		lamportLogicClockLabel = new JLabel();
		lamportLogicClockLabel.setBounds(240, 140, 250, 20);
		lamportLogicClockLabel.setText("Lamport Logic Clock  : " + clock);
		userPanel.add(lamportLogicClockLabel);
		
		mainPanel.add(userPanel);
		
		//获取本地IP地址
		this.localIpAddress = getLocalIP();
		
		chatGroupMap = new HashMap<Integer, ChatGroup>();
		//开启监听周围组发送会话组消息线程
		new Thread(new SystemHandler()).start();
		//进行扫描，发现周围是否存在会话组
		searchGroup();
		
	}

	// 创建组按钮监听器，点击"create group"按钮回调这里的函数
	private ActionListener createGroupListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			//先检查网络环境
			if(localIpAddress == null){
				JOptionPane.showConfirmDialog(frame, "当前没有连接网络，请检查网络连接，然后重启该程序", 
						"网络错误",JOptionPane.CLOSED_OPTION);
				return;
			}
			// 首先提示选择Order Algorithm
			Object[] options = { "Total", "Causal" };
			int n = JOptionPane.showOptionDialog(frame, "Please Choose One:",
					"Order Algorithm", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			//返回 -1 说明 没有选择任何一个
			if (n != -1) {
				// 设置isTototal
				isTotalOrder = ((n == 0) ? true : false);

				// 提示设置用户名
				if (userName == null)
					setUserName();
				System.out.println(userName);
				if ((userName == null) || (userName == ""))
					return;
				// 提示设置端口
				createGroupPort();
				if((groupPort <20000) ||(groupPort > 60000)){
					JOptionPane.showConfirmDialog(frame, "端口号请在【20000-60000】", "错误", 
							JOptionPane.CLOSED_OPTION);
					userName = null;
					return;
				}
				// 避免端口重复
				if (chatGroupMap.containsKey(groupPort)) {
					JOptionPane.showConfirmDialog(frame, "错误，该组已经存在了", "错误", 
							JOptionPane.CLOSED_OPTION);
					userName = null;
					return;
 				}
				// 设置isLeader，首次创建组的用户默认为当前组的leader
				isLeader = true;
				// 登记当前创建的会话组
				currentGroup = new ChatGroup(groupPort, localIpAddress,
						Protocal.user_tcp_port, isTotalOrder);
				
				//检查创建一个组的信息是否完整
				if (isUserInfoComplete()) {
					// 实例化一个用户
					mChatUserInstance = new ChatUserInstance(ChatRoomFrame.this);
					// 设置标记
					isTalking = true;
					//刷新状态栏显示
					refreshStatus();
					//使能按钮
					createButton.setEnabled(false);
					joinButton.setEnabled(false);
					leaveButton.setEnabled(true);
				}
			}
		}
	};
	
	// 离开组按钮监听器，点击"leave group"按钮回调这里的函数
	private ActionListener leaveGroupListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// 清理用户信息
			userName = null;
			groupPort = 0;
			isLeader = false;
			isTotalOrder = false;
			currentGroup = null;
			isTalking = false;

			// 释放用户实例
			mChatUserInstance.freeInstance();
			mChatUserInstance = null;
	
			//按钮使能
			createButton.setEnabled(true);
			joinButton.setEnabled(true);
			leaveButton.setEnabled(false);
			//刷新状态
			refreshStatus();
		}
	};

	// 加入组按钮监听器，点击"join group"按钮回调这里的函数
	private ActionListener joinGroupListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			//先检查网络环境
			if(localIpAddress == null){
				JOptionPane.showConfirmDialog(frame, "当前没有连接网络，请检查网络连接，然后重启该程序", 
						"网络错误",JOptionPane.CLOSED_OPTION);
				return;
			}
			//发送探测消息，扫描当前是否存在用户组
			searchGroup();
			try {
				//暂停半秒，等待消息会送
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			// 加载获得的扫描信息
			Object[] options = chatGroupMap.keySet().toArray();
			//当前是否有会话组
			if (options.length == 0) {
				JOptionPane.showConfirmDialog(frame, "No Group on Line",
						"Sorry", JOptionPane.CLOSED_OPTION);
				return;
			}
			//提示选择要加入的会话组端口
			Integer group = (Integer) JOptionPane.showInputDialog(frame,
					"Please Choose One Port:", "Online Group",
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			//根据选择的端口提取该会话组的信息
			if (chatGroupMap.containsKey(group)) {
				ChatGroup chatGroup = chatGroupMap.get(group);
				if (chatGroup != null) {
					//设置当前会话组端口
					groupPort = chatGroup.getGroupPort();
					currentGroup = chatGroup;
				}
			} else {
				return;
			}

			// 设置用户名
			if (userName == null)
				setUserName();
			if (userName != null) {
				//设置标记
				isLeader = false;
				isTotalOrder = currentGroup.isTotalOrder;
				//实例化一个加入者
				mChatUserInstance = new ChatUserInstance(ChatRoomFrame.this);
				
				isTalking = true;
				refreshStatus();
				//按钮使能
				createButton.setEnabled(false);
				joinButton.setEnabled(false);
				leaveButton.setEnabled(true);
			}
		}
	};

	//	 发送消息按钮监听器，点击"send"按钮回调这里的函数
	private ActionListener sendMessageListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// 检查当前是否加入会话
			if (!isTalking) {
				JOptionPane.showMessageDialog(frame,
						"当前没有加入任何会话，请Create or Join加入一个组");
				return;
			}
			else {
				//组装要发送的消息，设置消息类型：request_chat
				Message msg = new Message(Protocal.request_chat);
				//设置消息信息：键入输入区的文字
				msg.setMessageInfo(msgWritedTextArea.getText());
				//将消息通过用户实例方法发送出去
				mChatUserInstance.sendGroupTalkMessage(msg);
			}
			//清空输入区
			msgWritedTextArea.setText("");
		}
	};

//	 取消消息按钮监听器，点击"cancel"按钮回调这里的函数
	private ActionListener cancelMessageListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			msgWritedTextArea.setText("");
		}
	};

	// 设置用户名：要么使用系统时间（以十六进制显示），要么自己输入。
	private void setUserName() {
		int n = JOptionPane.showConfirmDialog(frame, "是否自己定义用户名？",
				"UserNameSetting", JOptionPane.YES_NO_OPTION);
		if (n == -1) {
			userName = null;
			return;
		} else if (n == 0) {
			userName = JOptionPane.showInputDialog(frame, "Name");
		} else {
			userName = String.valueOf(Long.toHexString(System
					.currentTimeMillis()));
		}
	}

	// 设置要创建的会话组端口
	private void createGroupPort() {
		groupPort = Integer.valueOf(JOptionPane.showInputDialog(frame,
				"选择一个端口创建会话组[20000-60000]"));
	}

	// 搜寻周围是否存在会话组
	private void searchGroup() {
		//组装消息，设置消息类型： search_group
		Message msg = new Message(Protocal.search_group);
		// 通过applicationSocket发送出去
		systemMessageSender(msg);
	}

	// 刷新用户状态
	public  void refreshStatus() {
		userNameLabel.setText("UserName : " + userName);
		isLeaderLabel.setText("isLeader : " + isLeader);
		isTotalOrderLabel.setText("isTotalOrder ： " + isTotalOrder);
		if(currentGroup != null){
			currentGroupLabel.setText(currentGroup.toString());
		}
		else {
			currentGroupLabel.setText("currentGroup :  Null");
		}
		isTalkingLabel.setText("isTalking: " + isTalking);
	}

	// 通过applicationSocket 发送消息
	private void systemMessageSender(Message msg) {
		// 输出流
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oout = null;
		try {
			//将要发送的消息转换成规定格式 byte[]
			oout = new ObjectOutputStream(bout);
			oout.writeObject(msg);
			oout.flush();
			byte[] sendBuffer = bout.toByteArray();
			
			// 组装UDP数据报
			DatagramPacket packet = new DatagramPacket(sendBuffer,
					sendBuffer.length,
					InetAddress.getByName(Protocal.app_udp_ip),
					Protocal.app_global_upd_port);
			// 发送
			applicationSocket.send(packet);
			// 后台打印
			System.out.println("Send Search Group Message : " + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 接收 组搜寻消息，并反馈。
	class SystemHandler implements Runnable {
		@Override
		public void run() {
			while(true){
				byte[] receiveBuffer = new byte[2048];
				DatagramPacket packet = new DatagramPacket(receiveBuffer,
						receiveBuffer.length);
				try {
					// 接收消息
					applicationSocket.receive(packet);
					// 判断是否是自己发出的搜寻组消息
					if(!packet.getAddress().getHostAddress().contains(localIpAddress))
					{
						//输入流
						ByteArrayInputStream bin = new ByteArrayInputStream(
								receiveBuffer);
						ObjectInputStream oin = new ObjectInputStream(bin);
						//读入对象
						Object obj = oin.readObject();
						//判断是否是消息
						if (obj.getClass() == Message.class) {
							//对消息进行处理
							handleMessage((Message) obj);
						}
					}
					//休眠一会
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				} 
				}
			}
	}
	
	// 处理消息
	private void handleMessage(Message msg) {
		switch (msg.getMessageType()) {
		case Protocal.search_group:
			//打印消息
			System.out.println("recevie  Search Group Message  : " + msg.toString());
			//是否在会话
			if (isTalking) {
				//应答消息，设置消息类型：search_group_ack
				Message ackmsg = new Message(Protocal.search_group_ack);
				//设置消息内容： 当前组的信息
				ackmsg.setMessageInfo(currentGroup.toString());
				//发送消息
				systemMessageSender(ackmsg);
			}
			break;

		case Protocal.search_group_ack:
			//分解字符串
			//反馈的组信息格式： groupPort + "#" + leaderIp + "#" + leaderPort + "#"+ isTotalOrder
			String[] groupInfo = msg.getMessageInfo().split("#");
			//判断当前这个反馈组是否已经添加到记录
			if (!chatGroupMap.containsKey(Integer.valueOf(groupInfo[0]))) {
				//添加记录
				chatGroupMap.put(Integer.valueOf(groupInfo[0]),
						new ChatGroup(Integer.valueOf(groupInfo[0]),
								groupInfo[1], Integer.valueOf(groupInfo[2]),
								Boolean.valueOf(groupInfo[3])));
			}
			break;
		default:
			break;
		}
	}

	// 检查创建一个组所需的信息是否完整
	private boolean isUserInfoComplete() {
		if ((userName == null) || (groupPort < 20000) || (groupPort > 50000))
			return false;
		return true;
	}

	// 获取当前使用的IP,仅限有线连接
	private  String getLocalIP() {
		Enumeration<NetworkInterface> e;
		try {
			e = NetworkInterface
					.getNetworkInterfaces();
		} catch (SocketException e1) {
			e1.printStackTrace();
			return null;
		}
		String ipString = null;
		while (e.hasMoreElements()) {
			NetworkInterface ni = (NetworkInterface) e.nextElement();
			String name = ni.getName();

			if (name.contains("eth") || name.contains("wlan") /*Debain*/
					|| name.contains("Ethernet adapter")  
					|| name.contains("Wireless LAN adapter")  /*window*/
					|| name.contains("enp2s0f")) /*CentOs*/{
				Enumeration<InetAddress> inetEnum = ni.getInetAddresses();

				while (inetEnum.hasMoreElements()) {
					InetAddress address = (InetAddress) inetEnum.nextElement();
					//检查是否是IPv4
					if (address instanceof Inet4Address) {
						ipString = address.toString().substring(1);
					}
				}
			}
		}
		return ipString;
	}
	
	// 显示用户实例的当前时钟
	public void displayClock(long time){
		this.clock = time;
		lamportLogicClockLabel.setText("Lamport Logic Clock  : " + clock);
	}
	//显示收到消息
	public void displayMessage(Message msg){
		this.msgListModel.addElement("[" + msg.getMessageOwner()
				+ "] says : " + msg.getMessageInfo()
				+"[ ts ：" + msg.getMessageId().getTimeStamp() + "  ]");
	}
	
	
// 以下都是私有成员变量的Get or Set 方法
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getGroupPort() {
		return groupPort;
	}

	public void setGroupPort(int groupPort) {
		this.groupPort = groupPort;
	}

	public boolean isLeader() {
		return isLeader;
	}

	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}

	public boolean isTotalOrder() {
		return isTotalOrder;
	}

	public void setTotalOrder(boolean isTotalOrder) {
		this.isTotalOrder = isTotalOrder;
	}

	public boolean isTalking() {
		return isTalking;
	}

	public void setTalking(boolean isTalking) {
		this.isTalking = isTalking;
	}

	public ChatUserInstance getmChatUserInstance() {
		return mChatUserInstance;
	}

	public void setmChatUserInstance(ChatUserInstance mChatUserInstance) {
		this.mChatUserInstance = mChatUserInstance;
	}

	public ChatGroup getCurrentGroup() {
		return currentGroup;
	}

	public void setCurrentGroup(ChatGroup currentGroup) {
		this.currentGroup = currentGroup;
	}

	public HashMap<Integer, ChatGroup> getChatGroupMap() {
		return chatGroupMap;
	}

	public void setChatGroupMap(HashMap<Integer, ChatGroup> chatGroupMap) {
		this.chatGroupMap = chatGroupMap;
	}
	
	public String getLocalIpAddress() {
		return localIpAddress;
	}

	public void setLocalIpAddress(String localIpAddress) {
		this.localIpAddress = localIpAddress;
	}
}
