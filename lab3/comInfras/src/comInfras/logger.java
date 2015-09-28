package comInfras;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class logger extends Thread{
	public MessagePasser mp;
	Host log;
	public logger(MessagePasser msgP){
		this.mp=msgP;
		this.log= this.mp.serverHost;
	}
	
	public void run(){
		StringBuilder welcome = new StringBuilder();
		welcome.append("*** This is a logger. ***\n");
		System.out.println(welcome);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		// user interface, write in 
		while(true){
			System.out.println("\n* * * * * * * * * * * * * * *");
			System.out.println("main wait, type your command:");
			System.out.println("Usage-");
			System.out.println("1.print msg: to print message");
			System.out.println("bye: to exit ");
			System.out.println(">");
			try {
				String command = br.readLine();
				if (command.startsWith("bye")){
					System.out.println("end");
					System.exit(0);
				}
				else if (command.startsWith("1")){
					// print messages
					this.mp.printMsg();
					
				}
			} catch (IOException e) {
				System.out.println("main error!");
				e.printStackTrace();
			}
		}
	}
}
