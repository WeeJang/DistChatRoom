package edu.jangwee.component;
/**
 * 
 *  * @author Jang Wee
 *      会话组类
 *
 */
public class ChatGroup {
	//会话组端口
	private int groupPort;
	//会话组leader's IP
	private String leaderIp;
	//会话组leader's Port 
	private int leaderPort;
	// 会话组order algorithm
	// true : total order ; false :  causal order
	public boolean isTotalOrder;

	public ChatGroup(int groupPort, String leaderIp, int leaderPort,
			boolean isTotalOrder) {
		this.groupPort = groupPort;
		this.leaderIp = leaderIp;
		this.leaderPort = leaderPort;
		this.isTotalOrder = isTotalOrder;
	}

	public int getGroupPort() {
		return groupPort;
	}

	public void setGroupPort(int groupPort) {
		this.groupPort = groupPort;
	}

	public String getLeaderIp() {
		return leaderIp;
	}

	public void setLeaderIp(String leaderIp) {
		this.leaderIp = leaderIp;
	}

	public int getLeaderPort() {
		return leaderPort;
	}

	public void setLeaderPort(int leaderPort) {
		this.leaderPort = leaderPort;
	}

	public boolean isTotalOrder() {
		return isTotalOrder;
	}

	public void setTotalOrder(boolean isTotalOrder) {
		this.isTotalOrder = isTotalOrder;
	}

	@Override
	public String toString() {
		return groupPort + "#" + leaderIp + "#" + leaderPort + "#"
				+ isTotalOrder;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ChatGroup)) {
			return false;
		}
		ChatGroup cg = (ChatGroup) obj;

		return (this.groupPort == cg.groupPort)
				&& (this.leaderIp == cg.leaderIp)
				&& (this.leaderPort == cg.leaderPort)
				&& (this.isTotalOrder == cg.isTotalOrder);
	}

}
