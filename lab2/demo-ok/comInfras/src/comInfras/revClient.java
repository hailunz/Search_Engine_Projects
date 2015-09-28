package comInfras;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class revClient extends Thread{
	Socket clientsocket=null;
	MessagePasser mp;
	String dest=null;
	public revClient(Socket s,MessagePasser msgP, String dest){
		this.clientsocket=s;
		this.mp=msgP;
		this.dest=dest;
	}
	
	public Rules checkRevRules(Message msg, List<Rules> rulesList ){
		synchronized(this.mp.confFile){
		for (Rules rule: rulesList){
			if (rule.matchRules(msg)){
				return rule;
			}
		}
		return null;
		}
	}
	
	public void run(){
		
			timeStampMessage revMsg = null;

			ObjectInputStream objInput= null;
		
			String name = null;
			//System.out.println("First time get connection");
			
			
				try {
					objInput = new ObjectInputStream(this.clientsocket.getInputStream());
				
				
						while(true){
						
							revMsg=(timeStampMessage)objInput.readObject();
							System.out.println("revMsg received: 1 new message!\n"+revMsg.toString());
							name=revMsg.src;
							if (dest == null){	
								this.mp.sendCachePut(name,new ObjectOutputStream(this.clientsocket.getOutputStream()));
								dest=name;
							}

							
							messageHandler msgHand= new messageHandler(this.mp,revMsg);
							msgHand.start();
							//this.mp.revNormal(revMsg);
						
							System.out.println("receive over");
						}
				}catch (Exception e) {
					System.out.println("revMsg error outer: socket closed!");
					if (dest !=null){					
						this.mp.sendCacheRemove(dest);	
					}
					System.out.println("revMsg: remove socket from sendCache!");
					
					//e.printStackTrace();
					return;
				}
		
	}
}
