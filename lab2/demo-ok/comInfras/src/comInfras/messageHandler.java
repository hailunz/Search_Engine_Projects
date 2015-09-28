package comInfras;

import java.net.Socket;

public class messageHandler extends Thread{
	
	MessagePasser mp;
	timeStampMessage revMsg;
	
	public  messageHandler(MessagePasser msgP,timeStampMessage m){
		this.mp=msgP;
		this.revMsg=m;
	}
	
	public void run(){
		this.mp.revNormal(revMsg);
		
	}
}
