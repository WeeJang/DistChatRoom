package edu.jangwee.message;

import java.io.Serializable;

public class Message implements Serializable {
	
	private static final long serialVersionUID = -3286564461647015367L;
	// 消息类型
	private int messageType;
	// 消息Id
	private MessageId messageId;
	// 消息发送者
	private String messageOwner;
	// 消息内容
	private String messageInfo;
	// 是否ack了
	private boolean isAcked;
	

	public boolean isAcked() {
		return isAcked;
	}

	public void setAcked(boolean isAcked) {
		this.isAcked = isAcked;
	}

	public Message(int messageType) {
		this.messageType = messageType;
		//初始化一个值
		this.messageId = new MessageId();
		this.isAcked = false;
	}

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public MessageId getMessageId() {
		return messageId;
	}

	public void setMessageId(MessageId messageId) {
		this.messageId = messageId;
	}

	public String getMessageOwner() {
		return messageOwner;
	}

	public void setMessageOwner(String messageOwner) {
		this.messageOwner = messageOwner;
	}

	public String getMessageInfo() {
		return messageInfo;
	}

	public void setMessageInfo(String messageInfo) {
		this.messageInfo = messageInfo;
	}

	@Override
	public String toString() {
		return "messageType: " + messageType + " messageId: " + messageId
				+ " messageOwner: " + messageOwner + " messageInfo: "
				+ messageInfo;
	}

}
