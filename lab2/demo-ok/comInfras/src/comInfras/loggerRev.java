package comInfras;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class loggerRev extends Thread{
	Socket clientsocket=null;
	MessagePasser mp;
	String dest=null;
	public loggerRev(Socket s,MessagePasser msgP, String dest){
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
		
			Rules matchRule =null;
			Message newMsg= null;
			timeStampMessage revMsg = null;
			ObjectInputStream objInput= null;
			ObjectInputStream objOutput= null;
			String name = null;
			//System.out.println("First time get connection");
			
			try {
				objInput = new ObjectInputStream(this.clientsocket.getInputStream());
				
				while(true){
					try {
						//newMsg=(Message)objInput.readObject();
						revMsg=(timeStampMessage)objInput.readObject();
						System.out.println("revMsg received: 1 new message!");
						//System.out.println(newMsg.toString());
						
						newMsg = (Message) revMsg;
						
						name=newMsg.src;
						if (dest == null){	
							this.mp.sendCachePut(name,new ObjectOutputStream(this.clientsocket.getOutputStream()));
							dest=name;
						}
	
						this.mp.addMsg(revMsg);
						
						
						System.out.println("receive over");
		
					} catch (ClassNotFoundException e) {
						System.out.println("revMsg error: cannot readObject!");
						return;
						//e.printStackTrace();
					}catch (Exception e){
						System.out.println("revMsg error: socket closed!");
						if (dest !=null){	
							this.mp.sendCacheRemove(dest);	
						}
						System.out.println("revMsg: remove socket from sendCache!");
						System.out.println(this.mp.sendCache.isEmpty());
						return;
					}
				}
			} catch (IOException e) {
				System.out.println("revMsg error outer: socket closed!");
				if (dest !=null){	
					this.mp.sendCacheRemove(dest);	
				}
				System.out.println("revMsg: remove socket from sendCache!");
				System.out.println(this.mp.sendCache.isEmpty());
				return;
				//e.printStackTrace();
			} catch (Exception e){
				System.out.println("revMsg error outer: socket closed!");
				if (dest !=null){					
					this.mp.sendCacheRemove(dest);	
				}
				System.out.println("revMsg: remove socket from sendCache!");
				
			//	e.printStackTrace();
				return;
			}
				
	}
}
