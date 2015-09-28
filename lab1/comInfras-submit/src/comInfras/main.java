package comInfras;


/*
 * 18842 lab0
 * team 32
 * Hailun Zhu, ID: hailunz; 
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class main {

	public static void main(String[] args) throws IOException {
		
		StringBuilder welcome = new StringBuilder();
		welcome.append("*** Welcome to the User Interface! ***\n");
		welcome.append("*** This is a Communications Infrastructure. ***\n");
		welcome.append("*** To use this program, follow the instructions below. ***\n");
		
		System.out.println(welcome);
				
 		System.out.println("Give configuration file path:");
		BufferedReader readcom = new BufferedReader(new InputStreamReader(System.in));
		String filePath =null;
		try {
			 filePath = readcom.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		System.out.println("* Give local name:");
		//BufferedReader readcom = new BufferedReader(new InputStreamReader(System.in));
		String lname=null;
		try {
			lname=readcom.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		System.out.println("* Give clock type: logical or vector");
		//BufferedReader readcom = new BufferedReader(new InputStreamReader(System.in));
		String clocktype=null;
		try {
			clocktype=readcom.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		MessagePasser mp= new MessagePasser( filePath,lname, clocktype);
		
		//System.out.println("modified:"+mp.confFile.lastModifyTime);
		//test messages
//		Message no1= new Message("yujing","test","hello");
//		Message no2= new Message("charlie","test1","world");
		
		if (lname.equals("logger")){
			logger log = new logger(mp);
			log.start();
			return;
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String dest=null,kind=null,data=null;
		// user interface, write in 
		while(true){
			System.out.println("\n* * * * * * * * * * * * * * *");
			System.out.println("main wait, type your command:");
			System.out.println("Usage-");
			System.out.println("1.send: to send message");
			System.out.println("2.receive: to receive message");
			System.out.println("3.non-message event!");
			System.out.println("4.next event's time stamp.");
			System.out.println("5.send and log.");
			System.out.println("6.receive and log.");
			System.out.println("bye: to exit ");
			System.out.println(">");
			try {
				String command = br.readLine();
				if (command.startsWith("bye")){
					System.out.println("end");
					System.exit(0);
				}
				else if (command.startsWith("1")){
				//send	
					System.out.println("send!");
					//Message msg = new Message();
					System.out.print("give dest:");		
					dest=br.readLine();
					System.out.print("give kind:");
					kind=br.readLine();
					System.out.print("give data:");
					data=br.readLine();
					Message msg = new Message(dest,kind,data);
					mp.send(msg);
				}
				else if (command.startsWith("2")){
					//send	
						System.out.println("receive!");
						mp.receive();
					}
				else if (command.startsWith("3")){
					System.out.println("Non message event!");
					mp.newEvent();
				}
				else if (command.startsWith("4")){
					System.out.println("next event's time stamp:"+mp.clockser.ts.toString());
					
				}
				else if (command.startsWith("5")){
					System.out.println("send and log!");
					mp.toLogger=true;
					System.out.print("give dest:");		
					dest=br.readLine();
					System.out.print("give kind:");
					kind=br.readLine();
					System.out.print("give data:");
					data=br.readLine();
					Message msg = new Message(dest,kind,data);
					mp.send(msg);
					
				}else if (command.startsWith("6")){
					//send	
					System.out.println("receive and log!");
					mp.toLogger=true;
					mp.receive();
				}
				else{
						System.out.println("Wrong command!");
					}
			} catch (IOException e) {
				System.out.println("main error!");
				e.printStackTrace();
			}
		}
			
		}				
}
