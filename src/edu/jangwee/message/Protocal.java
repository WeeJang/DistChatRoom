package edu.jangwee.message;

public class Protocal {

	// UDP:
	// Ip
	public final static String app_udp_ip = "255.255.255.255";
	public final static int app_global_upd_port = 60001;
	// search
	public final static int search_group = 0xF0;
	public final static int search_group_ack = 0xF1;

	// order
	public final static int request_chat = 0x01;// only u loss
	public final static int request_chat_ack = 0x02;
	public final static int release_chat = 0x03;
	public final static int release_chat_ack = 0x04;

	// election
	public final static int elect = 0x20;
	public final static int elect_ack = 0x21;
	public final static int elect_finish = 0x22;
	//选举超时
	public final static int elect_time_out = 6000;

	// init
	public final static int request_init_info = 0x30;
	public final static int request_init_info_ack = 0x31;
	// leave
	public final static int leave_client_info = 0x32;

	// TCP 
	public  final static int user_tcp_port = 60002;

	public final static int request_process_id = 0x40;
	public final static int request_process_id_ack = 0x41;
	
	//Message
	//释放消息的时间戳，比较特殊，不会根据当前发送时间，便于在排队中优先被处理
	public final static long release_msg_ts = 1;
	
}
