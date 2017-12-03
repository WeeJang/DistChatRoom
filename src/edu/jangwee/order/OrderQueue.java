package edu.jangwee.order;

/**
 * @author JangWee
 * 定义 OrderQueue 类
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.PriorityBlockingQueue;

import edu.jangwee.message.AckMessage;
import edu.jangwee.message.Message;
import edu.jangwee.message.MessageId;

public class OrderQueue {
	// 持有者
	private Object owner;
	// 消息队列
	private PriorityBlockingQueue<Message> mQueue;
	// 清理队列时，暂存数据
	Stack<Message> mStack;

	public OrderQueue(Object owner, Comparator<Message> comparator) {
		this.owner = owner;
		this.mQueue = new PriorityBlockingQueue<Message>(48, comparator);
		this.mStack = new Stack<Message>();
	}

	// 获取该消息队列的比较器
	@SuppressWarnings("unchecked")
	public Comparator<Message> getComparator() {
		return (Comparator<Message>) mQueue.comparator();
	}

	// 检查队列为空
	public boolean isEmpty() {
		return this.mQueue.isEmpty();
	}

	// 检索队列头部元素，但不会取出
	public Message checkTopMessage() {
		return mQueue.peek();
	}

	// 将消息插入消息队列,这里保证了过滤后加入消息队列的消息不会有重复
	// 消息，不是应答自己的ack需要在接受时就要过滤掉
	public void insertMessage(Message msg) {
		// 是否存在
		if (!mQueue.contains(msg)) {
			while (!mQueue.offer(msg)) {
			}
		}
	}

	// 取出队列头部元素，并移出
	public Message getTopMessage() {
		return mQueue.poll();
	}

	// 取出优先级最高的消息。
	// total order返回消息的只有一个; casual可能有多个
	public ArrayList<Message> getTopMessageList() {
		// 判断消息队列是否为空
		if (getSize() == 0)
			return null;
		// 创建列表
		ArrayList<Message> topList = new ArrayList<Message>();
		// top消息
		Message topMessage = null;

		while (true) {
			// 取出
			topMessage = getTopMessage();
			// 添加到列表
			topList.add(topMessage);
			// 查看是否还有下一消息
			if (checkTopMessage() == null)
				break;
			// 如果下一个消息的优先级低了，取消息结束
			if (getComparator().compare(topMessage, checkTopMessage()) != 0)
				break;
		}
		// 将取出来的消息，重新添加回队列
		mQueue.addAll(topList);
		// 返回最高优先级消息的副本
		return topList;
	}

	// 根据MessageId查询在队列中的对应消息,查到就立即返回
	public Message getMessageById(MessageId msgId) {
		Message temp = null;
		// 队列迭代器
		Iterator<Message> iterator = mQueue.iterator();
		while (iterator.hasNext()) {
			Message msg = (Message) iterator.next();
			if (msg.getMessageId().equals(msgId)) {
				temp = msg;
				break;
			}
		}
		return temp;
	}

	// 根据消息副本，从队列中删除该对应消息
	public void removeMessage(Message msg) {
		mQueue.remove(msg);
	}

	// 根据 MessageId 删除与相关的所有消息
	public void takeCleanAboutMessage(MessageId messageId) {
		while (!mQueue.isEmpty()) {
			Message newMessage = getTopMessage();
			if (newMessage instanceof AckMessage) {
				// 是否是与messageId对应的ack消息
				if (!((AckMessage) newMessage).getAckMessageId().equals(
						messageId)) {
					// 不是，则寄存到栈里
					mStack.push(newMessage);
				}
			} else {
				// 是否是与messageId 对应的原始消息
				if (newMessage.getMessageId().equals(messageId)) {
					mStack.push(newMessage);
				}
			}
		}
		// 把栈里暂存的消息，恢复到队列
		while (!mStack.isEmpty()) {
			mQueue.put(mStack.pop());
		}
	}

	// 查询自己发出的self_ msg 获得的ack 数量
	// ack_type : 标示 回复的消息类型
	public int getAckedNumbers(Message self_msg, int ack_msg_type) {
		HashSet<Long> ackProcessSet = new HashSet<Long>();
		Iterator<Message> iterator = mQueue.iterator();
		while (iterator.hasNext()) {
			Message msg = iterator.next();
			// 判断消息类型
			if (msg.getMessageType() == ack_msg_type) {
				AckMessage ack = (AckMessage) msg;
				// 判断是否针对selfi_msg的消息回复
				if ((self_msg.getMessageId().equals(ack.getAckMessageId()))) {
					ackProcessSet.add(ack.getMessageId().getProcessId());
				}
			}
		}
		// 返回回复数量
		return ackProcessSet.size();
	}

	// 返回队列中当前消息数
	public int getSize() {
		return this.mQueue.size();
	}

	// get or set
	public Object getOwner() {
		return owner;
	}

	public void setOwner(Object owner) {
		this.owner = owner;
	}

	public PriorityBlockingQueue<Message> getmQueue() {
		return mQueue;
	}

	public void setmQueue(PriorityBlockingQueue<Message> mQueue) {
		this.mQueue = mQueue;
	}

}
