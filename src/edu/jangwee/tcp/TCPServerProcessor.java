package edu.jangwee.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

import edu.jangwee.component.ChatUserInstance;
import edu.jangwee.message.Message;
import edu.jangwee.message.Protocal;

/**
 * 服务端监听处理者
 * 
 * @author JangWee
 *
 */
public class TCPServerProcessor implements Runnable, ITCPProcessor {
	// 服务器端套接字
	private ServerSocket mServerSocket;
	// 连接的客户端数目
	// 存储分配进程号 String: IpAddress Long:ProcessId
	private HashMap<String, Long> indexNoteMap;
	// 存储对处理线程的引用 Long : ProcessId Runnable TCPServerWorker
	private HashMap<Long, Runnable> serverWorderMap;
	// 线程控制
	private boolean isProcessorAlive = false;
	// 进程序号分配器
	public  static Long  idCreator = Long.valueOf(4);
	// 拥有者
	private Object owner;

	public TCPServerProcessor(Object owner) {
		this.owner = owner;
		try {
			this.mServerSocket = new ServerSocket();
			//地址重用，保证选举后，重用端口
			this.mServerSocket.setReuseAddress(true);
			//绑定到监听端口
			this.mServerSocket.bind(new InetSocketAddress(Protocal.user_tcp_port));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.indexNoteMap  = new HashMap<String, Long>();
		this.serverWorderMap = new  HashMap<Long, Runnable>();
		this.isProcessorAlive = true;
	}

	// 初始化
	@Override
	public void initialize() {
		new Thread(this).start();
	}

	@Override
	public void run() {
				while (isProcessorAlive) {
				// 先预先准备一个分配进程号
				Long processId =  ++ idCreator  ;
				// 检查是否该进程号是否已经被分配
				if (serverWorderMap.get(processId) != null) {
					// 重新选择
					continue;
				}
				// 堵塞，等待客户端接入
				Socket clientSocket = null;
				try {
					clientSocket = mServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
				}
				//客户端接入
				System.out.println("有一个人加入组 ： " + clientSocket.getLocalAddress().getHostAddress());
				// 获取client的Ip
				String clientIp = clientSocket.getInetAddress().toString().substring(1);
				if (clientIp == null) {
					continue;
				}
				// 启动客户端处理线程
				Runnable serverWorker = null;
				try {
					serverWorker = new TCPServerWorker(this,
							processId, clientSocket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				// 客户端处理者启动
				new Thread(serverWorker).start();
				// 客户端信息存储
				indexNoteMap.put(clientIp, processId);
				serverWorderMap.put(processId, serverWorker);
				// 更新
				updateOwnerData(clientIp, processId);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
	}
		
	// 杀死所有子线程
	private void destoryAllWorker() {
		Iterator<Runnable> iter = serverWorderMap.values().iterator();
		while (iter.hasNext()) {
			TCPServerWorker worker = (TCPServerWorker) iter.next();
			worker.quitWorker();
		}
	}

	@Override
	public void quitProcessor() {
		// 先杀死worker线程
		destoryAllWorker();
		// 自身退出
		this.isProcessorAlive = false;
		//关闭套接字
		if(mServerSocket != null){
			try {
				mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}

	@Override
	public void handleMessage(Message msg) {
	}

	@Override
	public void sendMessage(Message msg) {

	}
	
	// 获取宿主对象
	public Object getOwner() {
		return this.owner;
	}
	
	// 获取当前时间
	public Long getCurrentTime() {
		return ((ChatUserInstance) this.owner).getCurrentTime();
	}
	
	//更新组员信息
	private void updateOwnerData(String ipAddress, Long processId) {
		((ChatUserInstance) this.owner).addGroupUser(ipAddress, processId);
	}
	
	//更新时钟
	public void updateCurrentTime(Message msg) {
		((ChatUserInstance) this.owner).updateTimeStamp(msg);
	}

	//清理客户端记录
	public void removeClientNote(String ipAddress){
		Long pId = indexNoteMap.get(ipAddress);
		if(pId != null){
			serverWorderMap.remove(pId);
		}
			
		indexNoteMap.remove(ipAddress);
	}
}


