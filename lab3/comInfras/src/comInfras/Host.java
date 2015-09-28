package comInfras;

import java.util.List;
import java.util.HashMap;



/*
 * 18842 lab0
 * team 32
 * Hailun Zhu, ID: hailunz; 
 */
public class Host {
	public String name;
	public String ip;
	public int port;
	public String voteSet;
	public HashMap<String,groupClock> groupsClock;
	public List<String> members;
	
}
