package edu.jangwee.order;
/**
 * @author Jang Wee
 * 定义TotalOrderQueue类
 * 
 */
import java.util.Comparator;

import edu.jangwee.message.Message;

public class TotalOrderQueue extends OrderQueue {

	public TotalOrderQueue(Object owner) {
		super(owner,new Comparator<Message>() {
			@Override
			public int compare(Message msg1, Message msg2) {
				//先比较时间戳，时间戳相同则比较进程号，保证total order排序
				if (msg1.getMessageId().getTimeStamp() == msg2.getMessageId()
						.getTimeStamp()) {
					return Long.compare(msg1.getMessageId().getProcessId(),
							msg2.getMessageId().getProcessId());
				} else {
					return Long.compare(msg1.getMessageId().getTimeStamp(),
							msg2.getMessageId().getTimeStamp());
				}
			}
		});
	}

}
