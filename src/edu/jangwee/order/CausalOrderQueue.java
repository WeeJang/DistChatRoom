package edu.jangwee.order;
/**
 * @author Jang Wee
 * 定义 CausalOrderQueue类
 */

import java.util.Comparator;

import edu.jangwee.message.Message;

public class CausalOrderQueue extends OrderQueue {
	
	public CausalOrderQueue(Object owner) {
		super(owner,new Comparator<Message>() {
			//仅仅按照时间戳对消息进行排序
			public int compare(Message msg1, Message msg2) {
				return Long.compare(msg1.getMessageId().getTimeStamp(), msg2
						.getMessageId().getTimeStamp());
			}
		});
	}

}
