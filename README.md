
# DistChatRoom

这是本科大四时候帮外国友人写的<<Distributed System>>课程作业（不得不说，国外的课程作业难度比国内高出不少),
学到了很多分布式系统相关的东西。第一次接触到了Lamport大神（对！就是那个Paxos算法的提出者）。

作业主要是实现一个分布式的聊天室，涉及到的功能：会话组成员的更新（加入／离开），选举算法，消息顺序一致性算法等。 
Clock主要是使用Lamport Logic Clock。 

## Prerequisites && Start 

安装前准备：
1 至少四台在在同一个局域网的主机，而且必须是通过有线进行连接的。无线的局域网组播，尝试没成功。

2 主机配置： 推荐window平台。主要是方便进行设置。

   2.1 首先关闭防火墙。不知道则百度之。

   2.2 搭建java 运行环境。主要是配置环境变量。这个你学过java，应该知道。
建议用我给你发过去的包。因为我是在这个JDk版本[java_1.70.65]上开发的。当然比这个版本高的应该没问题。低版本我不能保证。当然如果电脑上已经装了的话，可以先跑一下，没有问题，就不用替代了。具体的环境变量配置，参考《JRE环境变量配置》		
然后 进入cmd  输入 java -version 输出版本信息就okay了。

   2.3 如果想看源代码，可以导入eclipse. 解压 ChatRoom.zip ，然后 import 导入工程，因为里面的一些程序注释是中文，我的编码用的是UTF-8，如果中文现实乱码的话， 百度一下 “eclipse utf-8 设置” 就会了，设置一下自己的eclipse

3 运行 chatroom.jar 

Charroom.jar 是 ChatRoom 工程文件打包导出的可执行文件。打开 cmd , 切到 该jar文件所在目录，然后，键入 “java -jar chatroom.jar” 即可执行文件。UI界面就加载了，cmd后台会打印一些我开发和调试过程中信息，通过这些信息，你就可以看到他们之间的通信。
当然，你也可以直接在eclipse 里运行，import工程后，F11即可。


## Required Features

Specifically it must have the following features:

1. A user should be able to create a group

2. A user should be able to join an existing group

3. A user should be able to leave a group (both graceful and ungraceful)

4. The users of a group should be able to elect a group leader in a distributed manner

5. Implement two different communication methods, choosing which one the program
will use at startup
a. Total order of the messages
b. Casual order of the messages

## Design And implements

1 A user should be able to create a group 。UI 界面上 CreateGroup.根据提示，键入用户名，尽量英文，比如 host ，不想输入的话，点击否，它会自动生成一个名字，是根据当前时间转化成十六进制的。然后输入一个端口,提示是20000-60000。电脑的0 -1023端口是为系统，1024-65535是可以用的，我就化了一个范围。60000-65536的尽量不要用，是因为我这是给TCP保留的，下面的TCP链接要用。输入正确之后，就建立了。

2 A user should be able to join an existing group。UI界面上的JoinGroup. 当然前提是当前局域网内有一台chatroom客户端在运行，才能加入。其他同上。

3 A user should be able to leave a group (both graceful and ungraceful)。这个的话，就是说 graceful 是 点击 LeaveGroup , ungraceful 就是直接关闭窗口，或者拔掉网线。
如果这个客户端不是leader的话，就用LeaveGroup，，展示graceful 。是leader的话，直接关闭窗口，展示ungraceful。这个地方开发的还不完整。我会再改正。

4 The users of a group should be able to elect a group leader in a distributed manner
 这个的话，这样展示。三台机子进入同一个组播，然后关闭 leader，注意一定要直接关闭窗口，就是 ungraceful 的方式，因为 我用的TCP自带的心跳监听，延迟很大，有超时限制，相应很慢。而leader直接关闭，则非leader进程就会立即知道（它里面有堵塞线程，抛出异常），然后开始选举。选举算法就 Bully Algorithm.进程大的会成为新的leader,然后给其他进程重新分配processId（在自己的基础上递增）。

5 Implement two different communication methods, choosing which one the program
will use at startup
a. Total order of the messages
b. Casual order of the messages
   这个的话，我用的方法跟推荐的不一样。我觉得不要紧，后面提到vector clock也是建议使用，只要实现这两肿排序即可。所以我都是用的Lamport Logical clock：思路来源 Lamport大神的《Lamport, L. (1978). “Time, clocks, and the ordering of events in a distributed system”》 和 《http://www.orzace.com/lamport-logical-clock/》。

   ```
   Casul order ： 只要保证因果就好。
   Total order :  保证一致性全序。
   ```

开发这个程序的时候，第一要考虑清楚的是，竞争的资源是什么。这个分布式应用主要是围绕同步的。在这里，我定义竞争的资源就是”谁先在消息栏里显示”，就像一般的分布式架构中，竞争的资源是”谁先将日志写入服务器”，不能出现先结帐后下单的情况。
对应的在这个程序里，就不能出现b回复A的消息先于A的消息在在消息栏里现实。

我就直接抛出实现的结果：
   选择了Total Order.在整个应用中，消息都是这样比较的：首先比较消息发出的时间，谁时间早，谁就排对排前面。如果发出的时间相等，则比较进程号，进程号小的排前面。这样保证了total .
   比如 A,B 同时在时间点 13 时发出 “hi”消息，因为A的进程号小于B所以，A比B先显示。在整个会话过程中，无论遇到多少次这种情况，永远都是A排在B前面。

然而，在causal order ,遇到上述情况时，不能保证 A 永远排在B前面，因为他俩是没有因果关系的，有可能有几次是A 在前，也有可能是B在前，因为casual order 只是根据 发出消息的时间点进行比较。A 与 B时间点相同，说明他俩没联系。倘若B是回复A的消息，B首先会提取A的时间戳，如果A的时间戳比自己的时钟大，就更新自己的时钟， 然后加快一步。这样就保证了只要是回复的消息，肯定比原始消息大。

需要明确的是，所有的进程看到的消息栏都是一样的。Casual order 并非意味着如果A B 在同一个时间说了一句话，所有参与进程看到显示顺序不一样，你看到的A在前,我看到的B在前，这是不可能的。因为显示之前先排队，有可能的是 这两条消息到达的顺序是不一样的，有可能是你是先收到A的消息，我先收到是B的消息。但最终显示的顺序大伙是一样的。可以把这个消息栏看作是一个公共大屏幕，大伙在大屏幕下唠磕，说话的内容都会在这个大屏幕上显示出来，不可能出现你看到大屏幕跟我看的不一样吧。Causal Order 会使得 可能统一时刻说出的话，显示顺序不一样。但Total Order 就能保证了，同一时刻一样的，按照学号排序！这样就解决了。
这也是 第二个网址内容的 意义。

当然 实现这个的方法前提是，保证消息的达到顺序与到达顺序一直。就是第二篇网址中提到的，”现在让我们回过头看看文章开头提到的问题，怎么利用Logical Clock保证日志的顺序正确呢？Lamport在论文的后半部分提出了一个算法，可以解决这个问题。但是这个算法基于一个前提：对于任意的两个进程Pi和Pj，它们之间传递的消息是按照发送顺序被接收到的。这个假设并不过分，TCP就可以满足要求。“


这也就是为什么困扰这么长时间，费了很大功夫的问题。要运用logic Clock的话，前提是TCP链接。而这个程序是要在UDP下做的，所以必须要包装一下，有类似TCP的功能，及确认和重传机制。而且是面向多链接的确认重传。比如你有四个要发送的对象，你广播出去，必须收到4个ack 才能算发送成功，不然就一直重传。”Only the chat messages needs to be sent with UDP. You can use TCP to retrieve the sequence numbers from the group leader
You can choose to have either acknowledgements or negative acknowledgments“
这个是实验要求。

因此有很多细节要处理，包括实时检测队列，检查ack，队列清理，组员维护，等等。幸好 java在处理多线程和并发上有较大的优势。我使用了PriorBlockingQueue来对收到的队列进行维护，通过添加比较器，它能对收到的消息及时排序，把优先级高的排到队首优先处理。消息排队的原则是前面已经提到，但是遇到要求将队列中的消息进行显示的release消息时，需要将该条release消息置顶，优先处理。

为了保证面向多链接的UDP确认重传，只能对收到的对话消息（协议中消息类型为 request_chat）的消息进行模拟丢包，迫使发送端重传。其他的消息类型，不会丢包。

因为丢包率很高，接近90%，而且为了演示我故意方面了线程的处理速度，所以，当发送一个消息时，没有立即看到显示，稍等一会，你看一下后台打印的数据，如果不是 “mqueue empty “ 就是在重传。一条消息的发送到显示有一下流程：
从非发送者看来： request_chat[发送者产生] ->request_chat_ack[自己回复] -> release_chat[发送者产生] ->显示消息
从发送者看来  ：equest_chat[自己产生] ->request_chat_ack[接收者回复] ->(全部接收者都回复消息了) ->release_chat[自己产生] -> release_chat_ack[接收者回复]-->(全部接收者都回复消息了)->显示消息


## Code

简单介绍一下源代码

ChatGroup.java 定义一个会话组： 会话组使用的端口，leader的IP,leader的port

ChatRoomFrame.java : 加载UI,程序窗口就是由它定义的，主要是一些控件，按钮。它还负责扫描周围有什么会话组。其实就是通过 一个固定的端口[app_global_upd_port] 广播一个包，任何收到包的就把自己的信息发过来。

ChatUserInstance.java ：产生一个用户实例。它有TCP工具，UDP工具，并且维护一个OrderQueue,是主要的类。

AckMessage.java 定义了回复消息的消息类型。

Message.java 定义了消息类型。

MessageId.java 定义了区分了消息的类。是消息的身份证。包含发送消息的进程的进程号以及时间戳。

Protocal.java 是定义了消息协议。Message中的messageType 就是从这些常量里面取值，UDP 和TCp消息，还有一些别的端口，地址。

CausalOrederQueue.java 定义了casual order的排队方式。

TotalOrderQueue.java 定义了 total order 的排队方式。

OrderQueue.java 是上面两个的父类，为主要是实现了OrdeQueuer的操作方法。

OrderQueueHandler.java 是维护队列的，相当于他维护这这个队列，添加消息到队列，从队列中取出消息进行处理等等。

TCPServerProcessor.java ： leader拥有的TCP服务端，当有新成员加入时，就会与之建立链接，这个类就会创建一个worker线程处理接入客户端的请求。

TCPServerWorker.java 就是上面提到的给Processor干活的。

TCPClientProcessor.java : 非leader类拥有的TCP客户端，与leader联系的通道，请求ProceeId,监听保持连接等。

TCPProcessor.java 一个接口，定义了TCPServerProcessor TCPServerWorker要实现的功能。

UDPProcessor.java  绝大部分消息的接收，发送，处理。其中 象资源请求（request_chat）资源请求回复（requset_ack） 资源释放（release_chat） 资源释放回复（release_ack）都是由 OrderQueueHandler 处理，UDPProcessor只负责接受这四类消息，将其插入消息队列。而剩余的消息 选举（elect） 选举回复（elect_ack）选举结束（elect_finish） 初始消息请求(request_init_info) 初始消息请求回复(request_init_info_ack) 都是UDPProcessor 负责的。


## reference

http://www.orzace.com/lamport-logical-clock/ 就明白我写的程序大概什么哥意思了。

http://wenku.baidu.com/link?url=oGRTdWBv1U1oQaD__6Adzo_WxnRVV9IEU6mIswbpBmFB4Jk_1keW_OnaWV5SxZW8FnJ4RWkZ_u8SBa20eV6BPLLXc8sPUqlCutlXaiH9X3a
这个文库也可以参考。

