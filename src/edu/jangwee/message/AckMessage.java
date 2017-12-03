package edu.jangwee.message;

import java.io.Serializable;

public class AckMessage extends Message implements Serializable {
	
	private static final long serialVersionUID = -3286564461647015368L;
	/* 标识答复的哪一条消息 */
	public MessageId ackMessageId;

	public AckMessage(int messageType, MessageId ackMessageId) {
		super(messageType);
		this.ackMessageId = ackMessageId;
	}

	public MessageId getAckMessageId() {
		return ackMessageId;
	}

	public void setAckMessageId(MessageId ackMessageId) {
		this.ackMessageId = ackMessageId;
	}

	@Override
	public String toString() {
		return super.toString() + " ackMessageId: " + ackMessageId;
	}
}
