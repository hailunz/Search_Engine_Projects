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
		
			Rules matchRule =null;
			Message newMsg= null;
			ObjectInputStream objInput= null;
			ObjectInputStream objOutput= null;
			String name = null;
			//System.out.println("First time get connection");
			
			try {
				objInput = new ObjectInputStream(this.clientsocket.getInputStream());
				
				while(true){
					try {
						newMsg=(Message)objInput.readObject();
						System.out.println("revMsg received: 1 new message!");
						//System.out.println(newMsg.toString());
						name=newMsg.src;
						if (dest == null){	
							this.mp.sendCachePut(name,new ObjectOutputStream(this.clientsocket.getOutputStream()));
							dest=name;
						}
	
							//check updates
							this.mp.updateRules();
							//checkrules
							matchRule = checkRevRules(newMsg,this.mp.confFile.receiveRules);
						
						//test:add to revbuffer
						if (matchRule==null){
							
							this.mp.revBufferOffer(newMsg);
							// check delay
							synchronized (this.mp.revDelayBuffer){
								while (this.mp.revDelayBuffer.size()>0){
								this.mp.revBufferOffer(mp.revDelayBuffer.poll());
								}
							}
						}//have matched some rule
						else{
							String act=matchRule.action;
							if (act.startsWith("dup")){
								
								System.out.println("revMsg: match dup:"+newMsg.toString());
								mp.revBufferOffer(newMsg);
								mp.revBufferOffer(newMsg.dupMsg());
								
								// check delay
							
								synchronized (this.mp.revDelayBuffer){
									while (this.mp.revDelayBuffer.size()>0){
									this.mp.revBufferOffer(mp.revDelayBuffer.poll());
									}
								}
							}else if (act.equals("drop")){
								System.out.println("revMsg: match drop:"+newMsg.toString());
								
								synchronized (this.mp.revDelayBuffer){
									while (this.mp.revDelayBuffer.size()>0){
									this.mp.revBufferOffer(mp.revDelayBuffer.poll());
									}
								}
							
								System.out.println("receive over");
								continue;
							}else if (act.equals("delay")){
								System.out.println("revMsg: match delay:"+newMsg.toString());
									mp.revDelayBufferOffer(newMsg);
							}else{
								System.out.println("revMsg:new message with a wrong rule action");
							}
						}
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
