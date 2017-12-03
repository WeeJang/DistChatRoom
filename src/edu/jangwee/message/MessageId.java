package edu.jangwee.message;

import java.io.Serializable;

public class MessageId implements Serializable {
	
	private static final long serialVersionUID = -3286564461647015365L;
	// 发送该消息的进程号
	private long processId;
	// 发送该消息对应的时间戳
	private long timeStamp;

	public MessageId(){
		//初始化一个值
		this.processId = 999999;
		this.timeStamp = 0;
	}
	
	public long getProcessId() {
		return processId;
	}

	public void setProcessId(long processId) {
		this.processId = processId;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public String toString() {
		return "processId: " + processId + " timeStamp: " + timeStamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (processId ^ (processId >>> 32));
		result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageId other = (MessageId) obj;
		if (processId != other.processId)
			return false;
		if (timeStamp != other.timeStamp)
			return false;
		return true;
	}

}
