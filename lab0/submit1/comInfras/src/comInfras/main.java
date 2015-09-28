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

		MessagePasser mp= new MessagePasser( filePath,lname);
		
		//System.out.println("modified:"+mp.confFile.lastModifyTime);
		//test messages
//		Message no1= new Message("yujing","test","hello");
//		Message no2= new Message("charlie","test1","world");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String dest=null,kind=null,data=null;
		// user interface, write in 
		while(true){
			System.out.println("\n* * * * * * * * * * * * * * *");
			System.out.println("main wait, type your command:");
			System.out.println("Usage-");
			System.out.println("1.send: to send message");
			System.out.println("2.receive: to receive message");
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
					}else{
						System.out.println("Wrong command!");
					}
			} catch (IOException e) {
				System.out.println("main error!");
				e.printStackTrace();
			}
		}
			
		}				
}
