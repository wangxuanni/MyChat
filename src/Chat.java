import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;


public class Chat {
	private static CopyOnWriteArrayList<Channel> all =new CopyOnWriteArrayList<Channel>();
	public static void main(String[] args) throws IOException {
		System.out.println("-----Server-----");
		// 1、指定端口 使用ServerSocket创建服务器
		ServerSocket server =new ServerSocket(8888);
		// 2、阻塞式等待连接 accept
		while(true) {
				Socket  client =server.accept(); 
				System.out.println("一个客户端建立了连接");
				Channel c =new Channel(client);
				all.add(c); //管理所有的成员
				new Thread(c).start();			
			}		
		}
		//一个客户代表一个Channel
		static class Channel implements Runnable{
			private DataInputStream dis;
			private DataOutputStream dos;
			private Socket  client;			
			private boolean isRunning;
			private String name;
			public Channel(Socket  client) {
				this.client = client;
				try {
					dis = new DataInputStream(client.getInputStream());
					dos =new DataOutputStream(client.getOutputStream());
					isRunning =true;
					//获取名称
					this.name =receive();
					//欢迎你的到来
					this.send("欢迎你的到来");
					sendOthers(this.name+"来了shsxt聊天室",true);
				} catch (IOException e) {
					System.out.println("---1------");
					release();					
				}			
			}
			//接收消息
			private String receive() {
				String msg ="";
				try {
					msg =dis.readUTF();
				} catch (IOException e) {
					System.out.println("---2------");
					release();
				}
				return msg;
			}
			//发送消息
			private void send(String msg) {
				try {
					dos.writeUTF(msg);
					dos.flush();
				} catch (IOException e) {
					System.out.println("---3------");
					release();
				}
			}
			/**
			 * 群聊：获取自己的消息，发给其他人
			 * 私聊: 约定数据格式: @xxx:msg
			 * @param msg
			 */
			private void sendOthers(String msg,boolean isSys) {
				boolean isPrivate = msg.startsWith("@");
				if(isPrivate) { //私聊
					int idx =msg.indexOf(":");
					//获取目标和数据
					String targetName = msg.substring(1,idx);
					msg = msg.substring(idx+1);
					for(Channel other: all) {
						if(other.name.equals(targetName)) {//目标
							other.send(this.name +"悄悄地对您说:"+msg);
							break;
						}
					}
				}else {				
					for(Channel other: all) {
						if(other==this) { //自己
							continue;
						}
						if(!isSys) {
							other.send(this.name +"对所有人说:"+msg);//群聊消息
						}else {
							other.send(msg); //系统消息
						}
					}
				}
			}
			//释放资源
			private void release() {
				this.isRunning = false;
				CloseUtils.close(dis,dos,client);
				//退出
				all.remove(this);
				sendOthers(this.name+"离开大家庭...",true);
			}
			@Override
			public void run() {
				while(isRunning) {
					String msg = receive() ;
					if(!msg.equals("")) {
						//send(msg);
						sendOthers(msg,false);
					}
				}
			}
		}
}
