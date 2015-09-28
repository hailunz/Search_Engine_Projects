package comInfras;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class MessagePasser {
	
	public configFile confFile;
	public String confName;
	public String localName;
	public String clockType;
	
	public int seqNum;
	//buffers
	public LinkedList<timeStampMessage> sendDelayBuffer;
	public LinkedList<timeStampMessage> revBuffer;
	public LinkedList<timeStampMessage> revDelayBuffer;
	
	public ConcurrentLinkedQueue <timeStampMessage> holdback;
	public ConcurrentLinkedQueue <timeStampMessage> waitDeliver;
	
	//public  lock
	
	public ReentrantLock lock;
	public ReentrantLock wlock;
	// server socket
	public ServerSocket server=null;
	public Host serverHost = null ;
	
	// cache connection
	public HashMap<String,ObjectInputStream > clientCache;
	public HashMap<String, ObjectOutputStream> sendCache;
	
	// store host with stamp id for vector clock
	public HashMap <String, Integer> timeStampId;
	public clockService clockser;
	public timeStamp timeS;
	public int hostNum;
	
	public boolean toLogger;
	public Host logger;
	
	//group
	public String group;
	
	//buffers for logger
		public HashMap<String, ArrayList<timeStampMessage>> msgBuffer;
		public List<List<timeStampMessage>> concurrent;
		public List<timeStampMessage> orderBuffer;
		
	//buffers for groups
		public HashMap<String,List<String>> groupMap;//group name: members
		
		// message: members | getack.
		public HashMap<String,HashMap<String,Boolean>> Received;
		
		// message: msg
		public HashMap<String,timeStampMessage> ReceivedBag;
		public ConcurrentLinkedQueue<timeStampMessage> multiCache;
		public HashMap<String,ConcurrentLinkedQueue<timeStampMessage>> multiCacheMap;
		public HashSet<String> deliverBag;
		
		//critical section
		public String state;
		public boolean vote;
		public ConcurrentLinkedQueue<timeStampMessage> requestBag;
		public HashSet<String> replyBag;
		
		public int sendNum;
		public int revNum;
		
	
	public MessagePasser(String configuration_filename, String local_name, String clock_type) throws FileNotFoundException {
		
		//parse file
		
		InputStream input = new FileInputStream(new File(configuration_filename));
		Constructor constructor = new Constructor(configFile.class);
		Yaml yaml = new Yaml(constructor);
		this.confFile =(configFile) yaml.load(input);
		
		this.confName = configuration_filename;
		this.localName=local_name;
		this.clockType=clock_type;
		
		File file = new File(this.confName);
		this.confFile.lastModifyTime = file.lastModified();
		
		for (Host tmp : this.confFile.configuration){
		
			if (tmp.name.equals(local_name)){
				this.serverHost=tmp;
				break;
			}
		}
		
		// check host
		if (serverHost == null)
		System.out.println("MP fails:wrong local host!");
		
		//initial other
		this.seqNum=0;
		this.revBuffer=new  LinkedList<timeStampMessage>();
		this.sendDelayBuffer=new  LinkedList<timeStampMessage>();
		this.revDelayBuffer=new  LinkedList<timeStampMessage>();
		
		this.holdback = new ConcurrentLinkedQueue<timeStampMessage>();
		
		this.clientCache= new  HashMap<String,ObjectInputStream>();
		this.sendCache = new HashMap<String,ObjectOutputStream>();
		
		this.server=createServer(local_name,serverHost.port);
		this.lock=new ReentrantLock();
		this.wlock=new ReentrantLock();
		
		this.concurrent= new ArrayList<List<timeStampMessage>>();
		this.msgBuffer= new HashMap<String, ArrayList<timeStampMessage>>();
		this.orderBuffer= new ArrayList<timeStampMessage>();
		
		
		// initial clock
		timeStampId=new HashMap<String, Integer>();
		
		// store the host name with their corresponding timeStampID
				int id=0;
				for (Host tmp : this.confFile.configuration){
					
					if (tmp.name.equals("logger")){
						logger=tmp;
						continue;
					}
						
					timeStampId.put(tmp.name, id++);
					this.msgBuffer.put(tmp.name,new ArrayList<timeStampMessage>());
					tmp.groupsClock= new HashMap<String,groupClock>();
					
				}

		hostNum=this.confFile.configuration.size()-1;
		this.timeS = new timeStamp();
		if (local_name.equals("logger")){
			
			if (clock_type.equals("logical")){
				this.timeS.id=-1;
				this.clockser= new logicalClock(this.timeS);
			}else{
				this.timeS.id=hostNum+1;
				this.clockser = new vectorClock(this.timeS, hostNum);
			}
			
		}else{
			if (clock_type.equals("logical")){
				this.timeS.id=timeStampId.get(this.serverHost.name);
				this.clockser= new logicalClock(this.timeS);
			}else{
				this.timeS.id=timeStampId.get(this.serverHost.name);
				this.clockser = new vectorClock(this.timeS, hostNum);
			}
		}
		
		toLogger=false;
		
		group = null;
		
		//initial groupMap
		this.groupMap=new HashMap<String,List<String>>();
		
		int tmpId=0;
		groupClock gclocks;
		timeStamp ts = new timeStamp();
		
		multiCacheMap = new HashMap<String,ConcurrentLinkedQueue<timeStampMessage>>();
		
		for (Group a: this.confFile.groups){
			
			this.multiCacheMap.put(a.name, new ConcurrentLinkedQueue<timeStampMessage>());
			this.groupMap.put(a.name, a.members);		
			tmpId=0;
			for (String tmp: a.members){
				if (tmp.equals(this.localName)){
					ts = new timeStamp();
					ts.id=tmpId;
					gclocks= new groupClock(ts,a.members.size());
					this.serverHost.groupsClock.put(a.name,gclocks);
					//System.out.println(this.serverHost.groupsClock.get(a.name).ts.vector+","+this.serverHost.groupsClock.get(a.name).ts.id);
				}
				tmpId++;
			}
			
		}
		
		this.serverHost.members = this.groupMap.get(this.serverHost.voteSet);
		
		 this.Received = new HashMap<String,HashMap<String,Boolean>> ();
		 this.ReceivedBag = new HashMap<String,timeStampMessage>();
		 
		 this.multiCache = new ConcurrentLinkedQueue<timeStampMessage>();
		 this.waitDeliver= new ConcurrentLinkedQueue<timeStampMessage>();
		 deliverBag = new HashSet<String>();
		
		 // critical sections
		 this.state="released";
		 this.vote= false;
		 this.requestBag= new ConcurrentLinkedQueue<timeStampMessage>();
		 this.replyBag = new HashSet<String>();
		 
		 this.sendNum=0;
		 this.revNum=0;
		  
		//start server thread
		myServer serverThread = new myServer(this.server,this);
		serverThread.start();
		
	}

	void newEvent(){
		System.out.println("new event time stamp:" + this.clockser.ts.toString());
		this.clockser.updateClock();
		System.out.println("next event time stamp:" + this.clockser.ts.toString());
	}
	
	void sendMsg(Message message){
		ObjectOutputStream objOutput=sendCache.get(message.dest);
			try {
				objOutput.writeObject(message);
				System.out.println("SendMSG ok: send!"+message.toString());
				
			} catch (IOException e) {
				System.out.println("SendMSG error: can't write!");
				e.printStackTrace();
			}
	}
	
	
	public Rules checkSendRules(Message msg, List<Rules> rulesList ){
		synchronized(this.confFile){
		for (Rules rule: rulesList){
			if (rule.matchRules(msg)){
				return rule;
				}
			}
		return null;
		}
	}
	
	
	// final send method
	void sendTimedMsg(timeStampMessage message){
		
		//add socket or get socket
		if (message == null || message.dest==null){
			System.out.println("Message format wrong!");
			return;
		}
		String dest=message.dest;
		Host rehost =this.confFile.getHost(dest);
		
		if(rehost==null){
			System.out.println("Send error: unknown host!");
			return;
		}
		if (!sendCache.containsKey(dest)){
			
			try {
				Socket tmpSoc= new Socket(rehost.ip,rehost.port);
				sendCachePut(dest,new ObjectOutputStream(tmpSoc.getOutputStream()));
				System.out.println("send: have add a new host to sendCache!\n");
				//notify the receiver of this connection				
				revClient client= new revClient(tmpSoc,this,dest);
				client.start();
				
			} catch (UnknownHostException e) {
				System.out.println("Send error: unknown host!");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Send error: cannot open socket!");
				e.printStackTrace();
			}
		}
	
		ObjectOutputStream objOutput=sendCache.get(message.dest);
			try {
				objOutput.writeObject(message);
				System.out.println("SendMSG ok: send!"+message.toString());
			} catch (IOException e) {
				System.out.println("SendMSG error: can't write!");
				e.printStackTrace();
			}
			
		//send to logger at the same time.
		if (this.toLogger){
			if (!sendCache.containsKey("logger")){
				// open connection with logger
				try {
					Socket tmpSoc= new Socket(logger.ip,logger.port);
					sendCachePut("logger",new ObjectOutputStream(tmpSoc.getOutputStream()));
					System.out.println("send: have add a new host to sendCache!\n");
					//notify the receiver of this connection				
					revClient client= new revClient(tmpSoc,this,"logger");
					client.start();
					
				} catch (UnknownHostException e) {
					System.out.println("Send error: unknown host!");
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("Send error: cannot open socket!");
					e.printStackTrace();
				}
				System.out.println(" add logger to sendCache!\n");
			}
				objOutput=sendCache.get("logger");
				this.toLogger=false;
				message.data=message.toString();
				message.dest="logger";
				try {
					objOutput.writeObject(message);
				} catch (IOException e) {
					System.out.println("SendMSG error: can't write to logger!");
				}
			}
			
	}
	
	// send one message, check rules, call sendTimedMsg
	void sendOne (timeStampMessage msg){
		if (!msg.kind.startsWith("multiack"))
			this.sendNum++;
		//check updates
		this.updateRules();
		//checkrules
		Rules matchRule = checkSendRules(msg,this.confFile.sendRules);

	
		if (matchRule == null){
			//send msg
			//sendMsg(message);
			sendTimedMsg(msg);
			synchronized (this.sendDelayBuffer){
				while(sendDelayBuffer.size()>0){
					timeStampMessage tsSend= sendDelayBuffer.poll();
					//sendMsg(send);
					sendTimedMsg(tsSend);
				}
			}
		}
		else{
			String act =matchRule.action;
			if (act.startsWith("delay")){
				System.out.println("send: match delay: "+msg.toString());
				this.sendDelayBufferOffer(msg);
			}else if (act.startsWith("dup")){	
				System.out.println("send: match dup: "+msg.toString());

				sendTimedMsg(msg);
				sendTimedMsg(msg.dupMsg());
				
				synchronized (this.sendDelayBuffer){
					while(sendDelayBuffer.size()>0){
						timeStampMessage tsSend= sendDelayBuffer.poll();
						//sendMsg(send);
						sendTimedMsg(tsSend);
					}
				}
				
			}else if (act.startsWith("drop")){
				System.out.println("send: match drop: "+msg.toString());
				synchronized (this.sendDelayBuffer){
					while(sendDelayBuffer.size()>0){
						timeStampMessage tsSend= sendDelayBuffer.poll();
						//sendMsg(send);
						sendTimedMsg(tsSend);
					}
				}
				return;
			}
		}
	}
	
	
	//send multicast 
	void Bmulticast(timeStampMessage msg){
	
		System.out.println("multicast "+ msg.toString());
		// send to others other than me.
		List<String> list = this.groupMap.get(msg.mGroup);
		
		for (String a: list){
			if (!a.equals(msg.src)){
				msg.dest=a;
				sendOne(msg);
			}		
		}
		
	}
	
	void send(Message message) {

	//	System.out.println("send this:"+message.toString());
		message.src=this.localName;
		if (message.dupe==null)
			message.dupe="false";
		
		//lock
			lock.lock();
			message.set_seqNum(this.seqNum++);
		//	System.out.println("1.send this:"+message.toString());
			lock.unlock();
			
			timeStampMessage tsMsg = new timeStampMessage(message);
			this.clockser.setClock(tsMsg.timeStamp);
			
			if (message.kind.startsWith("multicast")){
				
				tsMsg.mGroup=this.group;
				
				if (message.kind.equals("multicastrequest")){
					synchronized(this.state){
						this.state="wanted";
					}
					tsMsg.reqHost=this.localName;
					tsMsg.mGroup= this.serverHost.voteSet;
				}
				
				tsMsg.oriSrc=tsMsg.src;
				tsMsg.oriType=tsMsg.kind;
				
				if (!this.serverHost.groupsClock.containsKey(tsMsg.mGroup)){
					tsMsg.dest=this.groupMap.get(tsMsg.mGroup).get(0);
					tsMsg.kind="tomulti";
					sendOne(tsMsg);
					return;
				}
								
				groupClock c = this.serverHost.groupsClock.get(tsMsg.mGroup);
				c.updateClock();
				tsMsg.gtimeStamp=new timeStamp(c.ts);
				
				// add message I multicast to the multiCache
				ConcurrentLinkedQueue <timeStampMessage>multicache = this.multiCacheMap.get(tsMsg.mGroup);
				multicache.add(tsMsg);
				timeStampMessage ack = new timeStampMessage();
				ack.clone(tsMsg);
				ack.kind="multiackself";
				ack.dest=this.localName;
				
				// send myself ack
				sendOne(ack);
				System.out.println("send ack."+ack.toString());
				// send others multicast m.
				Bmulticast(tsMsg);
				while(!this.state.equals("released")){
					try {
						Thread.sleep(1000);
					//	System.out.println("state "+ this.state);
					//	System.out.print("");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				};
			}
			else{
				tsMsg.oriSrc=tsMsg.src;
				tsMsg.oriType=tsMsg.kind;
				sendOne(tsMsg);
			}
	}
	
	void sendRev2logger(timeStampMessage message){
		ObjectOutputStream objOutput;
		message.data=message.toString();
		message.src=this.localName;
		message.dest="logger";
		if (!sendCache.containsKey("logger")){
			// open connection with logger
			try {
				Socket tmpSoc= new Socket(logger.ip,logger.port);
				sendCachePut("logger",new ObjectOutputStream(tmpSoc.getOutputStream()));
				System.out.println("send: have add a new host to sendCache!\n");
				//notify the receiver of this connection				
				revClient client= new revClient(tmpSoc,this,"logger");
				client.start();
				
			} catch (UnknownHostException e) {
				System.out.println("Send error: unknown host!");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Send error: cannot open socket!");
				e.printStackTrace();
			}
			System.out.println(" add logger to sendCache!\n");
		}
			objOutput=sendCache.get("logger");
			//message.data=message.toString();
			try {
				objOutput.writeObject(message);
			} catch (IOException e) {
				System.out.println("SendMSG error: can't write to logger!");
			}
		
	}

	public Rules checkRevRules(Message msg, List<Rules> rulesList ){
		synchronized(this.confFile){
		for (Rules rule: rulesList){
			if (rule.matchRules(msg)){
				return rule;
			}
		}
		return null;
		}
	}
	
	void revMultiCast(timeStampMessage msg){
		
		System.out.println("Enter revMultiCast "+msg.toString());

		String message=msg.toMulti();
		if (deliverBag.contains(message)){
			System.out.println("delivered already.");
			return;
		}
		HashMap<String,Boolean> res;
		if (Received.containsKey(message)){
			res=this.Received.get(message);
			res.put(msg.src, true);

		}else{
			res = new HashMap<String,Boolean>();
			// set sender
			res.put(msg.src, true);
			this.Received.put(msg.toMulti(), res);
			this.ReceivedBag.put(msg.toMulti(), msg);
			
			timeStampMessage tsMsg = new timeStampMessage();
			tsMsg.clone(msg);
			tsMsg.src=this.localName;
			tsMsg.kind="multiack";
			tsMsg.dest=this.localName;
			
			sendOne(tsMsg);
			Bmulticast(tsMsg);
			
		}
	}
	
	void revMultiAck(timeStampMessage msg){
		System.out.println("Enter revMultiAck "+msg.toString());
		String message=msg.toMulti();
		
		if (deliverBag.contains(message)){
			System.out.println("delivered already.");
			return;
		}
		
		
		HashMap<String,Boolean> res;
		if (Received.containsKey(message)){
			res=this.Received.get(message);
			res.put(msg.src, true);
		}else{
			res = new HashMap<String,Boolean>();
			// set sender
			res.put(msg.src, true);
			res.put(msg.oriSrc, true);
			this.Received.put(msg.toMulti(), res);
			this.ReceivedBag.put(msg.toMulti(), msg);
			
			if (!msg.src.equals(msg.oriSrc)){
				timeStampMessage tsMsg = new timeStampMessage();
				tsMsg.clone(msg);
				tsMsg.src=this.localName;
				tsMsg.kind="multiack";
				tsMsg.dest=this.localName;
				
				sendOne(tsMsg);
				Bmulticast(tsMsg);
			}					
		}
		System.out.println(res.toString());
		
		
		//check res
		groupClock gclock = this.serverHost.groupsClock.get(msg.mGroup);
		gclock.displayClock();
		// all set
		if (res.size()== gclock.ts.vector.size()){
			//checkCO
			gclock.displayClock();
			boolean isCO=gclock.checkCO(msg.gtimeStamp);
			int msgID = msg.gtimeStamp.id;
			
			// multicast by me
			if (msg.oriSrc.equals(this.localName)){
				ConcurrentLinkedQueue<timeStampMessage> multicache= this.multiCacheMap.get(msg.mGroup);
				
				if (multicache.peek().toMulti().equals(message)){
					//deliver and update clock.
					System.out.println("deliver msg to waitDeliver:"+ msg.toString());
					multicache.poll();
					this.revBufferOffer(msg);
					this.checkReq(msg);
					this.checkRelease(msg);
					deliverBag.add(msg.toMulti());
					gclock.ts.vector.set(msgID, msg.gtimeStamp.vector.get(msgID));
					//After update the clock, check the message in the holdback queue
					for (timeStampMessage m : this.holdback){
						if(m.mGroup.equals(msg.mGroup)){
							if (multicache.peek().toMulti().equals(m.toMulti())){
								multicache.poll();
								//deliver and update clock.
								this.revBufferOffer(m);
								
								this.checkReq(m);
								this.checkRelease(m);
								
								deliverBag.add(m.toMulti());
								this.holdback.remove(m);
								gclock.ts.vector.set(m.gtimeStamp.id, m.gtimeStamp.vector.get(m.gtimeStamp.id));
								System.out.println("remove from holdback queue."+m.toString());
								gclock.displayClock();
							}
						}
					}
				}else{
					//add to the holdback queue
					System.out.println("add msg to holdback:"+ msg.toString());
					this.holdback.add(msg);
				}
				
			}else{
				// multicast by others
				if (isCO){
					//deliver and update clock.
					System.out.println("deliver msg to waitDeliver:"+ msg.toString());
					this.revBufferOffer(msg);
					
					this.checkReq(msg);
					this.checkRelease(msg);
					
					deliverBag.add(msg.toMulti());
					gclock.ts.vector.set(msgID, msg.gtimeStamp.vector.get(msgID));
					//After update the clock, check the message in the holdback queue
					for (timeStampMessage m : this.holdback){
						if(m.mGroup.equals(msg.mGroup)){
							if (gclock.checkCO(m.gtimeStamp)){
								//deliver and update clock.
								this.revBufferOffer(m);
								
								this.checkReq(m);
								this.checkRelease(m);
								
								deliverBag.add(m.toMulti());
								this.holdback.remove(m);
								gclock.ts.vector.set(m.gtimeStamp.id, m.gtimeStamp.vector.get(m.gtimeStamp.id));
								System.out.println("remove from holdback queue."+m.toString());
								gclock.displayClock();
							}
						}
					}
					
				}else{
					//add to the holdback queue
					System.out.println("add msg to holdback:"+ msg.toString());
					this.holdback.add(msg);
				}
				
				
			}
			
		
			
			gclock.displayClock();
			System.out.println("waitDeliver"+this.revBuffer.toString());
			System.out.println("holdback:"+this.holdback.toString());

		}
	}
	
	
	void Bdeliver(timeStampMessage msg){
		
		if (msg.kind.equals("tomulti")){
			timeStampMessage tsMsg = new timeStampMessage();
			tsMsg.clone(msg);
			//lock
			lock.lock();
			tsMsg.set_seqNum(this.seqNum++);
		//	System.out.println("1.send this:"+message.toString());
			lock.unlock();
			
			this.clockser.compareClock(msg.timeStamp);
			tsMsg.timeStamp= new timeStamp();
			this.clockser.setClock(tsMsg.timeStamp);
			
			tsMsg.src=this.localName;
			tsMsg.kind="multi";
			tsMsg.oriSrc=this.localName;
			groupClock c = this.serverHost.groupsClock.get(tsMsg.mGroup);
			c.updateClock();
			tsMsg.gtimeStamp=new timeStamp(c.ts);
			
			// add message I multicast to the multiCache
			ConcurrentLinkedQueue <timeStampMessage>multicache = this.multiCacheMap.get(tsMsg.mGroup);
			multicache.add(tsMsg);
			timeStampMessage ack = new timeStampMessage();
			ack.clone(tsMsg);
			ack.kind="multiackself";
			ack.dest=this.localName;
			
			// send myself ack
			sendOne(ack);
			System.out.println("send ack."+ack.toString());
			// send others multicast m.
			Bmulticast(tsMsg);
		}
		else if (msg.kind.startsWith("multicast")){
			this.revNum++;
			revMultiCast(msg);
			
		}else if (msg.kind.startsWith("multiack")){	
			revMultiAck(msg);
			
		}else if (msg.kind.equals("reply")){
			this.revNum++;
			this.revBufferOffer(msg);
			this.replyBag.add(msg.src);
			if (this.replyBag.containsAll(this.serverHost.members)){
				// receive all the reply, enter the cs
				this.state="held";
				this.replyBag.clear();
				this.criticalSec();
			}
		}
		else{
			this.revBufferOffer(msg);
		}
		
	}
	// receive message normal
	synchronized void revNormal(timeStampMessage newMsg){
		
		  System.out.println("Enter revNormal.");
			//check updates
			this.updateRules();
			//checkrules
			Rules matchRule = checkRevRules(newMsg,this.confFile.receiveRules);
		
			//test:add to revbuffer
			if (matchRule==null){
			
				Bdeliver(newMsg);
				// check delay
				synchronized (this.revDelayBuffer){
					while (this.revDelayBuffer.size()>0){
						 Bdeliver(this.revDelayBuffer.poll());
				}
			}
		}//have matched some rule
		else{
			String act=matchRule.action;
			if (act.startsWith("dup")){
				
				System.out.println("revMsg: match dup:"+newMsg.toString());
				 Bdeliver(newMsg);
				 Bdeliver(newMsg.dupMsg());
				
				// check delay
			
				synchronized (this.revDelayBuffer){
					while (this.revDelayBuffer.size()>0){
						 Bdeliver(this.revDelayBuffer.poll());
					}
				}
			}else if (act.equals("drop")){
				System.out.println("revMsg: match drop:"+newMsg.toString());
				
				synchronized (this.revDelayBuffer){
					while (this.revDelayBuffer.size()>0){
						 Bdeliver(this.revDelayBuffer.poll());
					}
				}
	
			}else if (act.equals("delay")){
				System.out.println("revMsg: match delay:"+newMsg.toString());
					this.revDelayBufferOffer(newMsg);
			}else{
				System.out.println("revMsg:new message with a wrong rule action");
			}
		}
			
			 System.out.println("exit revNormal.");
		}
	
	
	
	


	synchronized Message receive() {
		synchronized(this.revBuffer){
		  if (revBuffer.size()<1){
			  System.out.println("no waiting message to receive!");
			  return null;
		 }
		  timeStampMessage rev = null;
		  rev = revBuffer.poll();
		  
		  // update the clock
		  if (rev.dupe.equals("false"))
			  this.clockser.compareClock(rev.timeStamp);
		  
		  System.out.println(rev.toString());
		  
		  // if send to logger, send to logger
		  if (this.toLogger){
			  this.toLogger=false;
			  sendRev2logger(rev);
		  }
		  return rev;
		}
	}  
	
	// socket
	
	public ServerSocket createServer(String name, int port){
		ServerSocket serverSocket=null;
		 
		try {
			serverSocket= new ServerSocket(port);
			System.out.println("Create server:"+name +" ip: "+serverSocket.getLocalSocketAddress());
		} catch (IOException e) {
	
			System.out.println("Error:"+name + "fail to create server socekt!");
			e.printStackTrace();
		}
		return serverSocket;
		
	} 
	//update congfile
	public void updateRules(){
		synchronized(this.confFile){
		File file = new File(this.confName);
		if (this.confFile.lastModifyTime!= file.lastModified()){
			this.confFile.lastModifyTime=file.lastModified();
			
			InputStream input;
			try {
				input = new FileInputStream(new File(this.confName));
				Constructor constructor = new Constructor(configFile.class);
				Yaml yaml = new Yaml(constructor);
				this.confFile =(configFile) yaml.load(input);
			} catch (FileNotFoundException e) {
				
				System.out.print("updateRules error: can't update configuration files");
				e.printStackTrace();
			}
		}
		}
		
	}
	public void sendCacheRemove(String dest){
		synchronized(this.sendCache){
		this.sendCache.remove(dest);
		}
	}
	public void sendCachePut(String dest, ObjectOutputStream objOutput){
		synchronized(this.sendCache){
		this.sendCache.put(dest,objOutput);
		}
	}
	
	public void revBufferOffer(timeStampMessage msg){
		synchronized(this.revBuffer){
			this.revBuffer.offer(msg);
		}
	}
	public timeStampMessage revBufferPoll(){
		synchronized(this.revBuffer){
			return this.revBuffer.poll();
		}
	}
	
	public void revDelayBufferOffer(timeStampMessage msg){
		synchronized(this.revDelayBuffer){
			this.revDelayBuffer.offer(msg);
		}
	}
	public timeStampMessage revDelayBufferPoll(){
		synchronized(this.revDelayBuffer){
			return this.revDelayBuffer.poll();
		}
	}
	
	public void sendDelayBufferOffer(timeStampMessage msg){
		synchronized(this.sendDelayBuffer){
			this.sendDelayBuffer.offer(msg);
		}
	}
	public timeStampMessage sendDelayBufferPoll(){
		synchronized(this.sendDelayBuffer){
			return this.sendDelayBuffer.poll();
		}
	}
	
	void sortBuffer(List<timeStampMessage> list){
		Collections.sort(list,new Comparator<timeStampMessage>(){

			@Override
			public int compare(timeStampMessage o1, timeStampMessage o2) {
				int com;
				List<Integer> v1=o1.timeStamp.vector;
				List<Integer> v2=o2.timeStamp.vector;
				if (v1.equals(v2))
					return 0;
				for (int i=0;i<v1.size();i++){
					com=v1.get(i).compareTo(v2.get(i));
					if (com==0)
						continue;
					else
						return com;
				}
				return 0;
			}
    		
    	});
		
	}

/*
 * 	for critical section
 *  show state
 */
	void release(){
			 Message message = new Message(null,"multicastrelease","release");
			//		System.out.println("send this:"+message.toString());
			 message.src=this.localName;
			if (message.dupe==null)
				message.dupe="false";
			
			//lock
				lock.lock();
				message.set_seqNum(this.seqNum++);
			//	System.out.println("1.send this:"+message.toString());
				lock.unlock();
				
				timeStampMessage tsMsg = new timeStampMessage(message);
				this.clockser.setClock(tsMsg.timeStamp);
				
			//multicast release
				tsMsg.mGroup=this.serverHost.voteSet;
				tsMsg.oriSrc=tsMsg.src;
				tsMsg.oriType=tsMsg.kind;
				
				groupClock c = this.serverHost.groupsClock.get(tsMsg.mGroup);
				c.updateClock();
				tsMsg.gtimeStamp=new timeStamp(c.ts);
				
				// add message I multicast to the multiCache
				ConcurrentLinkedQueue <timeStampMessage>multicache = this.multiCacheMap.get(tsMsg.mGroup);
				multicache.add(tsMsg);
				timeStampMessage ack = new timeStampMessage();
				ack.clone(tsMsg);
				ack.kind="multiackself";
				ack.dest=this.localName;
				
				// send myself ack
				sendOne(ack);
				System.out.println("send ack."+ack.toString());
				// send others multicast m.
				Bmulticast(tsMsg);
	}

	synchronized void criticalSec(){
		System.out.println("!!! Enter critical Section !!!");
		System.out.println("Show critical state:");
		System.out.println("My state: "+ this.state);
		System.out.println("message num: send["+this.sendNum+"] receive ["+this.revNum+"] ");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String read="";
		
		while (!read.equals("yes")){
			System.out.println("Do you want to exit the critical section?");
			try {
				read=br.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		this.state="released";
		System.out.println("Leave critical Section"+this.state);
		release();
		
	}
	
	void checkReq(timeStampMessage msg){
			if (msg.oriType.equals("multicastrequest")){
				if (this.state.equals("held") || this.vote){
					System.out.println("Someone held the cs!");
					this.requestBag.add(msg);
				}else{

					this.vote=true;
					//send reply to the oriHost
					Message m = new Message (msg.reqHost,"reply","vote");
					System.out.println("vote for"+ msg.reqHost);
					this.send(m);
				}
			}
	}
	
	void checkRelease(timeStampMessage msg){
		if (msg.oriType.equals("multicastrelease")){
				if (this.requestBag.isEmpty()){
					this.vote=false;
					System.out.println("no request! my vote:"+ this.vote);
					System.out.println("no request! my state:"+ this.state);
					
				}else{
					timeStampMessage m = this.requestBag.poll();
					m.dest=m.oriSrc;
					m.kind="reply";
					m.data="vote";
					this.vote=true;
					System.out.println("poll a request! my vote:"+ this.vote +" to:" + m.dest);
					this.send(m);
				}

		}
	}
	
	
	
	/*
	 * for logger
	 */
	
	//for logger
	void addMsg(timeStampMessage msg){
		synchronized(this.msgBuffer){
			synchronized(this.orderBuffer){
			List <timeStampMessage> list = this.msgBuffer.get(msg.src);
			list.add(msg);
			this.orderBuffer.add(msg);
			
			//sort by timeStamp?
			sortBuffer(this.orderBuffer);
			
			// check concurrency
			String key;
			String res="";
			List<timeStampMessage> a;
			Iterator iter =this.msgBuffer.entrySet().iterator();  
			  while (iter.hasNext()) {  
		            HashMap.Entry<String, List<timeStampMessage>> entry = (HashMap.Entry<String, List<timeStampMessage>>) iter.next();   
		           // System.out.println(entry.getKey() + ": " + entry.getValue()); 
		            key=entry.getKey();
		            if (key.equals(msg.src)){
		            	continue;
		            }else{
		            	a=entry.getValue();
		            	for( timeStampMessage m: a){
		            		res=m.timeStamp.compareVector(msg.timeStamp);
		            		if (res.equals("concurrent")){
		            			List<timeStampMessage> tmp = new ArrayList<timeStampMessage>();
		            			tmp.add(m);
		            			tmp.add(msg);
		            			this.concurrent.add(tmp);
		            		}
		            			
		            	}
		            	
		            }
		      }
			
			}
		}
	}
	
	// for logger to print the message.
	void printMsg(){
		List <timeStampMessage> list;
		synchronized(this.msgBuffer){
			synchronized (this.orderBuffer) {
				//print the list of logged msg for each node
				System.out.println("Logged msg from each node");
				Iterator iter =this.msgBuffer.entrySet().iterator();  
				  while (iter.hasNext()) {  
			            HashMap.Entry<String, List<timeStampMessage>> entry = (HashMap.Entry<String, List<timeStampMessage>>) iter.next();   
			            System.out.println(entry.getKey() + ": " + entry.getValue());  
			      }
				  
				  System.out.println();
				  // print the ordered list based on arrival time;
				  System.out.println("All Logged messages!");
				 for (timeStampMessage a: this.orderBuffer){
					 System.out.println(a);
				 }
				  System.out.println();
				 //print the concurrent messages
				 System.out.println("concurrent messages!");
				 for (List a: this.concurrent){
					 System.out.println(a.get(0)+"||"+ a.get(1));
				 }
					
				}
				
			}
	}
}
