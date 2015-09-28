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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	
	//buffers for logger
		public HashMap<String, ArrayList<timeStampMessage>> msgBuffer;
		public List<List<timeStampMessage>> concurrent;
		public List<timeStampMessage> orderBuffer;
	
	public MessagePasser(String configuration_filename, String local_name, String clock_type) throws FileNotFoundException {
		
		/*
		 * if want to put the configuration file on a distributed website.
		 * I put it on Amazon S3:
		 * 	
		 */
//		URL url=null;
//		try {
//			url = new URL("https://s3.amazonaws.com/ds.lab0/test.yaml");
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		Constructor constructor = new Constructor(configFile.class);
//		Yaml yaml = new Yaml(constructor);
//		
//		InputStream input = null;
//		try {
//			input = url.openStream();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		this.confFile = (configFile) yaml.load(input);
//		
//		 this.confFile.printconfigFile(confFile);
		
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
	
	void sendTimedMsg(timeStampMessage message){
	
		ObjectOutputStream objOutput=sendCache.get(message.dest);
			try {
				objOutput.writeObject(message);
				System.out.println("SendMSG ok: send!"+message.toString());
			} catch (IOException e) {
				System.out.println("SendMSG error: can't write!");
				e.printStackTrace();
			}
			
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
	void send(Message message) {

	//	System.out.println("send this:"+message.toString());

		if (message == null || message.dest==null)
			return;
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
		
		message.src=this.localName;
		if (message.dupe==null)
			message.dupe="false";
		Rules matchRule = null;
		//lock
			lock.lock();
			message.set_seqNum(this.seqNum++);
		//	System.out.println("1.send this:"+message.toString());
			lock.unlock();
			
			//check updates
			this.updateRules();
			//checkrules
			matchRule = checkSendRules(message,this.confFile.sendRules);
	
			timeStampMessage tsMsg = new timeStampMessage(message);
			this.clockser.setClock(tsMsg.timeStamp);
		
			if (matchRule == null){
				//send msg
				//sendMsg(message);
				sendTimedMsg(tsMsg);
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
					System.out.println("send: match delay: "+tsMsg.toString());
					this.sendDelayBufferOffer(tsMsg);
				}else if (act.startsWith("dup")){	
					System.out.println("send: match dup: "+tsMsg.toString());

					sendTimedMsg(tsMsg);
					sendTimedMsg(tsMsg.dupMsg());
					
					synchronized (this.sendDelayBuffer){
						while(sendDelayBuffer.size()>0){
							timeStampMessage tsSend= sendDelayBuffer.poll();
							//sendMsg(send);
							sendTimedMsg(tsSend);
						}
					}
					
				}else if (act.startsWith("drop")){
					System.out.println("send: match drop: "+tsMsg.toString());
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

	synchronized Message receive() {
		synchronized(this.revBuffer){
		  if (revBuffer.size()<1){
			  System.out.println("no waiting message to receive!");
			  return null;
		 }
		  timeStampMessage rev = null;
		  rev = revBuffer.poll();
		  System.out.println(rev.toString());
		  if (this.toLogger){
			  this.toLogger=false;
			  sendRev2logger(rev);
		  }
		  return rev;
		}
	}  // may block.  Doesn't have to.
	
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
