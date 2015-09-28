package comInfras;


/*
 * 18842 lab0
 * team 32
 * Hailun Zhu, ID: hailunz; 
 */

import java.util.List;

public class configFile {
	
	public List<Group> groups;
	public List<Host> configuration;
	public List<Rules> sendRules;
	public List<Rules> receiveRules;

	long lastModifyTime;
	
	public configFile(){
		this.lastModifyTime=0;
	}
	public Host getHost(String dest){
		for (Host h:this.configuration){
			if (h.name.equals(dest))
				return h;
		}
		return null;
	}
	//not complete what if the ip is the same but the different port.
	public String getHostName(String ip){
		for (Host h:this.configuration){
			if (h.ip.equals(ip))
				return h.name;
		}
		return null;
	}
	
	public void printconfigFile(){
		for(Group a: this.groups){
			System.out.println("name:  "+a.name+"\nmembers:  "+a.members);
		}
		for(Host a: this.configuration){
			System.out.println("name:  "+a.name+"\nip:  "+a.ip+"\nport"+a.port);
		}
		for (Rules r: this.sendRules){
			System.out.println("send rules: action:  "+r.action+"  kind:  "+r.kind+ "src:  "+r.src);
		}
		for (Rules r: this.receiveRules){
			System.out.println("Rev rules: action:  "+r.action+"  kind:  "+r.kind+ "src:  "+r.src);
		}
	}
	
}
