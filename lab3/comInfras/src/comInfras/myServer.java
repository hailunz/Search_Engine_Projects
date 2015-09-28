package comInfras;


/*
 * 18842 lab0
 * team 32
 * Hailun Zhu, ID: hailunz; 
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class myServer extends Thread {
	
	public ServerSocket myserver=null;
	Socket csocket=null;
	MessagePasser mp;
	public myServer(ServerSocket s, MessagePasser msgP){
		this.myserver=s;
		this.mp=msgP;
	}
	public void run(){
			String address=null;
			String name=null;
			while(true){
				csocket=null;
				System.out.println("serverSocket:wait..");
				try {
					csocket=myserver.accept();
					//address=csocket.getRemoteSocketAddress().toString().split(":")[0];
					//name = this.mp.confFile.getHostName(address);
					if (this.mp.localName.equals("logger")){
						loggerRev client = new loggerRev(csocket,this.mp,null);
						client.start();
					}else{
						revClient client= new revClient(csocket,this.mp,null);
						client.start();
					}
					
					System.out.println("serverSocket: have created a new connection!"+csocket.getRemoteSocketAddress());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
		
}
