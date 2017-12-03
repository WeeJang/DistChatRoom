package edu.jangwee.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import edu.jangwee.component.ChatRoomFrame;
import edu.jangwee.component.ChatUserInstance;
import edu.jangwee.message.AckMessage;
import edu.jangwee.message.Message;
import edu.jangwee.message.MessageId;
import edu.jangwee.message.Protocal;

/**
 * UDP 处理者
 * @author Jang Wee
 *
 */
public class UDPProcessor implements Runnable {
	// socket
	private DatagramSocket mSocket;
	// 所属owner
	private Object owner;
	// 暂存当前获取的包的IP
	private InetAddress currentInetAddress;
	// 线程控制
	private boolean isProcessorRun;

	// 消息过滤
	// list 容量
	private static final int list_capacity = 100;
	// 记录消息【除release_chat】ID,过滤相同消息
	private ArrayList<MessageId> logMsgIdList;
	// logMsgIdList的索引，指示下一个空位置，便于复用这个list
	private int index_usual;
	// 记录消息【除release_chat】ID，过滤相同消息
	private ArrayList<MessageId> logReleaseAckMsgIdList;
	// logReleaseAckMsgIdList的索引
	private int index_release;

	// 随机数发生器
	private Random random;

	public UDPProcessor(Object owner, int port) throws SocketException {
		this.owner = owner;
		// 套接字初始化
		this.mSocket = new DatagramSocket();
		// 设置重用
		this.mSocket.setReuseAddress(true);
		// 绑定到监听端口
		this.mSocket.bind(new InetSocketAddress(port));

		this.index_usual = 0;
		this.index_release = 0;
		this.logMsgIdList = new ArrayList<MessageId>(list_capacity);
		this.logReleaseAckMsgIdList = new ArrayList<MessageId>(list_capacity);

		this.random = new Random();

		this.isProcessorRun = true;
	}

	// 初始化
	public void initialize() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (isProcessorRun) {
			// 接收缓冲区
			byte[] recvBuffer = new byte[4096];
			// 接收数据包
			DatagramPacket packet = new DatagramPacket(recvBuffer,
					recvBuffer.length);
			// 数据接收
			try {
				mSocket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// 判读是是自己发出的消息，不处理自己的消息
			if (!packet
					.getAddress()
					.getHostAddress()
					.contains(
							((ChatRoomFrame) ((ChatUserInstance) owner)
									.getOwner()).getLocalIpAddress())) {
				// 获取当前数据包的发送地址
				currentInetAddress = packet.getAddress();
				// 创建流
				ByteArrayInputStream bin = new ByteArrayInputStream(recvBuffer);
				ObjectInputStream oin = null;
				try {
					oin = new ObjectInputStream(bin);
				} catch (IOException e) {
					e.printStackTrace();
				}
				// 读取对象
				Object obj = null;
				try {
					obj = oin.readObject();
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
				// 检查对象类型
				if ((obj.getClass() == Message.class)
						|| (obj.getClass() == AckMessage.class)) {

					Message msg = (Message) obj;
					// 在这里模拟丢包
					if (msg.getMessageType() == Protocal.request_chat) {
						// 随机一个0-99的数，如果数字小于80，就丢弃。
						if (random.nextInt(99) < 80) {
							// 不处理，丢弃
							System.out
									.println("drop!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
							continue;
						}
					}

					// 阻止重复信息
					// 如果是release_chat消息
					if (msg.getMessageType() == Protocal.release_chat) {
						// 查看是否处理过
						if (!logReleaseAckMsgIdList.contains(((AckMessage) msg)
								.getAckMessageId())) {
							// 没有处理过就添加到记录
							logReleaseAckMsgIdList.add(index_release++,
									msg.getMessageId());
							// list 是否已经满了
							if (index_release == list_capacity)
								index_release = 0;
							// 处理消息
							handleMessage(msg);
						}
						// 初始化请求，及回复，不参与过滤
					} else if ((msg.getMessageType() == Protocal.request_init_info)
							|| (msg.getMessageType() == Protocal.request_init_info_ack)) {
						handleMessage(msg);
						// 其他类型的需要先过滤，然后处理
					} else {
						// 同上，消息过滤
						if (!logMsgIdList.contains(msg.getMessageId())) {
							logMsgIdList.add(index_usual++, msg.getMessageId());
							if (index_usual == list_capacity)
								index_usual = 0;
							handleMessage(msg);
						}
					}
				}
			}
		}
	}

	// UDP发送消息
	public void sendMessage(Message msg) {
		System.out.println("UDP 发送消息 :" + msg.toString());
		// 输出流
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oout = null;

		// 将数据转换成byte[]
		try {
			oout = new ObjectOutputStream(bout);
			oout.writeObject(msg);
			oout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] sendBuffer = bout.toByteArray();

		// 一个组共用一个port,也就是说，本地的socket要绑定到这个port上
		// 你发送给组的消息地址port也是这个
		DatagramPacket packet = null;
		try {
			packet = new DatagramPacket(sendBuffer, sendBuffer.length,
					InetAddress.getByName(Protocal.app_udp_ip),
					mSocket.getLocalPort());
			// 发送
			mSocket.send(packet);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 每发送一个消息更新时间
		((ChatUserInstance) this.owner).updateTimeStamp();
		// 发送完以后暂停一会
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleMessage(Message msg) {
		System.out.println(" UDP 处理消息 : " + msg.toString());
		ChatUserInstance userInstance = (ChatUserInstance) this.owner;

		AckMessage ackMsg = null;
		// 查看是否是回复消息
		if (msg instanceof AckMessage) {
			ackMsg = (AckMessage) msg;
		}
		// 根据收到消息对自己的时钟进行更新
		updateCurrentTime(msg);
		// 判断消息类型
		switch (msg.getMessageType()) {

		// 以下四类消息，进入OrderQueue，由对应的Handler 进行处理
		// 资源请求
		case Protocal.request_chat:
			// 插入消息队列
			userInstance.addMessageIntoQueue(msg);
			break;
		// 资源请求回复
		case Protocal.request_chat_ack:
			// 判断是否是针对自己消息的回复
			if (ackMsg.getAckMessageId().getProcessId() == userInstance
					.getProcessId()) {
				// 插入消息队列
				userInstance.addMessageIntoQueue(msg);
			}
			break;
		// 资源释放
		case Protocal.release_chat:
			userInstance.addMessageIntoQueue(msg);
			break;
		// 资源释放回复
		case Protocal.release_chat_ack:
			// 是否是ack自己的消息
			if (ackMsg.getAckMessageId().getProcessId() == userInstance
					.getProcessId()) {
				// 插入消息队列
				userInstance.addMessageIntoQueue(msg);
			}
			break;

		// 以下消息，不进入OrderQueue,立即直接处理
		// 通知选举
		case Protocal.elect:
			// 判断当前是否处于选举阶段
			if (!userInstance.isElection()) {
				userInstance.startElection();
			}
			// 判断当前是否已经丧失竞选资格
			if (!userInstance.isLoser()) {
				// 获取自己进程号
				Long selfProcessId = userInstance.getProcessId();
				// 如果自己进程号大于消息发送者，就参与竞选，并回复ack
				if (selfProcessId > msg.getMessageId().getProcessId()) {
					// 组装回复消息
					ackMsg = new AckMessage(Protocal.elect_ack,
							msg.getMessageId());
					ackMsg.getMessageId().setProcessId(selfProcessId);
					ackMsg.getMessageId().setTimeStamp(getCurrentTime());
					// 记录当前投递消息时间，便于计时
					userInstance.setElectionTime(System.currentTimeMillis());
					// udp发送消息
					sendMessage(ackMsg);
				}
			}
			break;
		// 竞选回复
		case Protocal.elect_ack:
			// 判断是否处于选举阶段
			if (userInstance.isElection()) {
				// 判断是否还有竞选资格
				if (!userInstance.isLoser()) {
					// 判断是否是回复的自己的消息
					if (userInstance.getProcessId() == ((AckMessage) msg)
							.getAckMessageId().getProcessId())
						// 设置标志位，表示自己落选
						userInstance.setLoser(true);
				}
			}
			break;
		// 竞选结束
		case Protocal.elect_finish:
			// 判断是否处于选举
			if (userInstance.isElection()) {
				// 标示选举结束
				userInstance.setElection(false);
				// 标示败选
				userInstance.setLoser(true);

				// 提取信息，新 leader的IP#Port
				String[] newLeaderInfo = msg.getMessageInfo().split("#");
				// 更新的关于leader信息
				userInstance.getCurrentChatGroup()
						.setLeaderIp(newLeaderInfo[0]);
				userInstance.getCurrentChatGroup().setLeaderPort(
						Integer.valueOf(newLeaderInfo[1]));
				// 设置自己的标志位，非leader
				userInstance.setLeader(false);
				// 复位
				userInstance.setLoser(false);
				userInstance.updateState();
				// 后续处理，重建TCP
				userInstance.afterElection();
			}
			break;
		// 搜寻组内成员
		case Protocal.request_init_info:
			// 添加新加入者的信息
			userInstance.addGroupUser(currentInetAddress.getHostAddress(), msg
					.getMessageId().getProcessId());

			// 把自身信息广播出去
			Message initMsg = new Message(Protocal.request_init_info_ack);
			// 用户名，进程号，ip可以根据消息自己提取
			initMsg.setMessageInfo(((ChatUserInstance) this.owner)
					.getUserName());

			MessageId msgId = new MessageId();
			msgId.setProcessId(((ChatUserInstance) this.owner).getProcessId());
			msgId.setTimeStamp(((ChatUserInstance) this.owner).getCurrentTime());
			initMsg.setMessageId(msgId);
			// udp发送消息
			sendMessage(initMsg);
			break;
		// 搜寻组内成员的回复
		case Protocal.request_init_info_ack:
			// 通过收到的包，提取对应IP,及进程号，添加到自己的组成员信息库
			userInstance.addGroupUser(currentInetAddress.getHostAddress(), msg
					.getMessageId().getProcessId());
			break;
		// 非leader用户离开
		case Protocal.leave_client_info:
			// 从组成员库中删除消息
			userInstance.removeGroupUser(msg.getMessageInfo());
			break;

		default:
			break;
		}
	}

	// 处理者退出
	public void quitProcessor() {
		// 设置标记
		this.isProcessorRun = false;
		// 关闭 Socket
		if (mSocket != null) {
			mSocket.close();
		}
	}

	// 得到当前时间戳
	private Long getCurrentTime() {
		return ((ChatUserInstance) this.owner).getCurrentTime();
	}

	// 更新时钟
	private void updateCurrentTime(Message msg) {
		((ChatUserInstance) this.owner).updateTimeStamp(msg);
	}
}
