package edu.jangwee.order;

/**
 * @author Jang Wee
 * OrderQueue 维护处理者
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

import edu.jangwee.component.ChatUserInstance;
import edu.jangwee.message.AckMessage;
import edu.jangwee.message.Message;
import edu.jangwee.message.MessageId;
import edu.jangwee.message.Protocal;

public class OrderQueueHandler implements Runnable {
	// 所属对象
	private Object owner;
	// 当前资源申请排队队列
	private OrderQueue mOrderQueue;
	// 当前处理的消息
	private Message currentTopMessage;
	// 标志位
	private boolean rechoose;
	// 线程控制
	private boolean isQueueHandlerRun;

	public OrderQueueHandler(Object owner, boolean isTotal) {
		this.owner = owner;
		new Stack<Message>();
		this.isQueueHandlerRun = true;
		this.rechoose = false;
		this.currentTopMessage = null;
		// 根据不同Order需求，实例化不同OrderQueue
		this.mOrderQueue = (isTotal ? new TotalOrderQueue(this)
				: new CausalOrderQueue(this));
	}

	// 初始化
	public void initiliaze() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (isQueueHandlerRun) {
			try {
				// 判断队列是否为空
				if (!mOrderQueue.isEmpty()) {
					// 返回最高优先级消息副本
					ArrayList<Message> list = mOrderQueue.getTopMessageList();
					// 打乱顺序，将消息顺序，主要是针对casual order,以便于处理多个同等级消息
					Collections.shuffle(list);
					// 消息迭代器
					Iterator<Message> iterator = list.iterator();
					while (iterator.hasNext()) {
						// 检查当前是否有重新筛选高等级消息通知
						if (rechoose) {
							rechoose = false;
							break;
						}
						// 获取消息
						currentTopMessage = (Message) iterator.next();
						// 进行处理
						handleMessage(currentTopMessage);
						Thread.sleep(20);
					}
				} else {
					// System.out.println(" Queue  Empty : ");
					// 队列为空，多睡一会
					Thread.sleep(1000);
				}
				currentTopMessage = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
				// 停止运行
				stopOrderQueueHandler();
			}
		}
	}

	// 停止消息队列处理者运行
	public void stopOrderQueueHandler() {
		this.isQueueHandlerRun = false;
	}

	//处理消息队列中的消息
	private void handleMessage(Message msg) {
		ChatUserInstance userInstance = (ChatUserInstance) this.owner;
		AckMessage ackMsg = null;

		switch (msg.getMessageType()) {
		// 资源申请【产生会话，申请显示】
		case Protocal.request_chat:
			// 是否是自己的资源申请
			if (isSelfMessage(msg)) {
				// 判断该申请是否得到其他成员全部答复
				if (this.mOrderQueue.getAckedNumbers(msg,
						Protocal.request_chat_ack) == userInstance
						.getGroupUser().size()) {
					//得到全部ack，发送release_chat消息,通知其他成员申请获得通过，可以显示消息
					AckMessage releaseMsg = new AckMessage(
							Protocal.release_chat, msg.getMessageId());
					MessageId msgId = new MessageId();
					msgId.setProcessId(userInstance.getProcessId());
					//	设置releaase_msg_ts,	便于在排队中优先被处理
					msgId.setTimeStamp(Protocal.release_msg_ts);
					releaseMsg.setMessageId(msgId);
					//往自身队列添加该消息
					this.mOrderQueue.insertMessage(releaseMsg);
					//将消息udp发送出去
					sendUDPMessage(releaseMsg);
				} else {
					//没有集齐7颗龙珠（全部ack）说明丢包 
					// 重传消息，继续发送 :-)
					sendUDPMessage(msg);
				}
				//不是自己的资源申请
			} else {
				//判断是否答复过该消息
				if (!msg.isAcked()) {
					msg.setAcked(true);
					//对该消息进行答复
					ackMsg = new AckMessage(Protocal.request_chat_ack,
							msg.getMessageId());
					// udp发送消息
					sendUDPMessage(ackMsg);
				}
			}
			break;
			
		//资源申请回复，表示收到资源申请
		case Protocal.request_chat_ack:
			//因为这条消息不应该出现在消息队列头部，有则直接删除
			this.mOrderQueue.removeMessage(msg);
			break;

		//资源释放回复，表示申请通过
		case Protocal.release_chat:
			AckMessage ackMessage = (AckMessage) msg;
			// 清理相关队列中的消息（资源申请，资源申请的ack）
			// 返回 资源申请的那条消息，可能为null
			Message requestMsg = cleanQueueByReleaseMessage(ackMessage);
			//判断是否为空
			if (requestMsg == null) {
				return;
			}
			// 资源释放，表示资源申请通过，在消息栏显示消息
			showMessage(requestMsg);
			
			// 判断该该资源请求是否是自己发起的
			if (isSelfMessage(requestMsg)) {
				//是自己的资源请求的释放，还需要确认是否所有成员都收到release_chat消息
				//再一次收集七颗龙珠
				if (mOrderQueue.getAckedNumbers(ackMessage,
						Protocal.release_chat) == userInstance.getGroupUser()
						.size()) {
					//ack都完整了，清理队列中的相关消息
					cleanAllAboutMessage(ackMessage.getAckMessageId());
				} else {
					//ack不够，重传 资源释放 消息
					sendUDPMessage(msg);
				}
			} else {
				// 不是自己的资源请求的释放
				if (!ackMessage.isAcked()) {
					// 设置ack标示
					ackMessage.setAcked(true);
					//对 release_chat进行回复
					AckMessage releaseAckMsg = new AckMessage(
							Protocal.release_chat_ack,
							ackMessage.getAckMessageId());
					// upd 发送
					sendUDPMessage(releaseAckMsg);
				}
			}
			//将该消息移除
			mOrderQueue.removeMessage(msg);
			break;

		case Protocal.release_chat_ack:
			mOrderQueue.removeMessage(msg);
			break;

		default:
			break;
		}
	}

	// 资源申请通过，展示消息
	private void showMessage(Message msg) {
		ChatUserInstance chatUserInstance = (ChatUserInstance) this.owner;
		chatUserInstance.updateUI(msg);
	}

	// 判断是否是自己发出的消息
	private boolean isSelfMessage(Message msg) {
		return (msg.getMessageId().getProcessId() == ((ChatUserInstance) this.owner)
				.getProcessId());
	}

	// 通过收到的消息对队列进行清理
	// release_chat 驱动 自己申请/他人申请清理
	// release_chat_ack 驱动 自己的release_chat清理
	// 返回对应的资源申请消息
	private Message cleanQueueByReleaseMessage(AckMessage ackmsg) {
		Message msg = null;
		Stack<Message> mStack = new Stack<Message>();
		while (!mOrderQueue.isEmpty()) {
			Message newMessage = mOrderQueue.getTopMessage();
			if (newMessage == null) {
				continue;
			}
			if (newMessage instanceof AckMessage) {
				if (!((AckMessage) newMessage).getAckMessageId().equals(
						ackmsg.getAckMessageId())) {
					mStack.push(newMessage);
				}
			} else {
				if (!newMessage.getMessageId().equals(ackmsg.getAckMessageId())) {
					mStack.push(newMessage);
				} else {
					// 显示，获得资源申请
					msg = newMessage;
				}
			}
		}
		while (!mStack.isEmpty()) {
			this.mOrderQueue.insertMessage(mStack.pop());
		}
		return msg;
	}

	//删除所有与 msgId相关的消息
	private void cleanAllAboutMessage(MessageId msgId) {
		this.mOrderQueue.takeCleanAboutMessage(msgId);
	}

	// 向owner递交消息
	private void sendUDPMessage(Message msg) {
		((ChatUserInstance) this.owner).sendGroupTalkMessage(msg);
	}

	// 向消息队列中插入消息
	public void addMessage(Message msg) {
		if (currentTopMessage != null) {
			if (this.mOrderQueue.getComparator()
					.compare(msg, currentTopMessage) <= 0) {
				//释放信号，暂停当前消息处理，重新检查队列中的最高优先级消息
				this.rechoose = true;
			}
		}
		this.mOrderQueue.insertMessage(msg);
	}

}
