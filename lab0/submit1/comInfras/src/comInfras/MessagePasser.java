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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class MessagePasser {
	
	public configFile confFile;
	public String confName;
	public String localName;
	
	public int seqNum;
	//buffers
	public LinkedList<Message> sendDelayBuffer;
	public LinkedList<Message> revBuffer;
	public LinkedList<Message> revDelayBuffer;
	//public  lock
	
	public ReentrantLock lock;
	public ReentrantLock wlock;
	// server socket
	public ServerSocket server=null;
	public Host serverHost = null ;
	
	// cache connection
	public HashMap<String,ObjectInputStream > clientCache;
	public HashMap<String, ObjectOutputStream> sendCache;
	
	public MessagePasser(String configuration_filename, String local_name) throws FileNotFoundException {
		
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
		
		File file = new File(this.confName);
	//	this.confFile.lastModifyTime = file.lastModified();
		
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
		this.revBuffer=new  LinkedList<Message>();
		this.sendDelayBuffer=new  LinkedList<Message>();
		this.revDelayBuffer=new  LinkedList<Message>();
		
		this.clientCache= new  HashMap<String,ObjectInputStream>();
		this.sendCache = new HashMap<String,ObjectOutputStream>();
		
		this.server=createServer(local_name,serverHost.port);
		this.lock=new ReentrantLock();
		this.wlock=new ReentrantLock();
		
		
		//start server thread
		myServer serverThread = new myServer(this.server,this);
		serverThread.start();
		
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
			System.out.println("1.send this:"+message.toString());
			lock.unlock();
			
			//check updates
			this.updateRules();
			//checkrules
			matchRule = checkSendRules(message,this.confFile.sendRules);
	
		
			if (matchRule == null){
				//send msg
				sendMsg(message);
				synchronized (this.sendDelayBuffer){
					while(sendDelayBuffer.size()>0){
					Message send= sendDelayBuffer.poll();
					sendMsg(send);
					}
				}
			}
			else{
				String act =matchRule.action;
				if (act.startsWith("delay")){
					System.out.println("send: match delay: "+message.toString());
					this.sendDelayBufferOffer(message);
				}else if (act.startsWith("dup")){	
					System.out.println("send: match dup: "+message.toString());
					sendMsg(message);
					sendMsg(message.dupMsg());
					
					
					synchronized (this.sendDelayBuffer){
						while(sendDelayBuffer.size()>0){
						Message send= sendDelayBuffer.poll();
						sendMsg(send);
						}
					}
					
				}else if (act.startsWith("drop")){
					System.out.println("send: match drop: "+message.toString());
					
					synchronized (this.sendDelayBuffer){
						while(sendDelayBuffer.size()>0){
							Message send= sendDelayBuffer.poll();
							sendMsg(send);
						}
					}
					
					return;
				}
			}
		
	}

	synchronized Message receive() {
		synchronized(this.revBuffer){
		if (revBuffer.size()<1){
			System.out.println("no waiting message to receive!");
			return null;
		}
		Message rev = null;
		rev = revBuffer.poll();
		System.out.println(rev.toString());
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
	
	public void revBufferOffer(Message msg){
		synchronized(this.revBuffer){
			this.revBuffer.offer(msg);
		}
	}
	public Message revBufferPoll(){
		synchronized(this.revBuffer){
			return this.revBuffer.poll();
		}
	}
	
	public void revDelayBufferOffer(Message msg){
		synchronized(this.revDelayBuffer){
			this.revDelayBuffer.offer(msg);
		}
	}
	public Message revDelayBufferPoll(){
		synchronized(this.revDelayBuffer){
			return this.revDelayBuffer.poll();
		}
	}
	
	public void sendDelayBufferOffer(Message msg){
		synchronized(this.sendDelayBuffer){
			this.sendDelayBuffer.offer(msg);
		}
	}
	public Message sendDelayBufferPoll(){
		synchronized(this.sendDelayBuffer){
			return this.sendDelayBuffer.poll();
		}
	}
}
