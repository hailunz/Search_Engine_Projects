package comInfras;


/*
 * 18842 lab0
 * team 32
 * Hailun Zhu, ID: hailunz; 
 */
public class Rules {
	
	public String action;
	public String src;
	public String dest;
	public String kind;
	public int seqNum=-1;
	public String duplicate;
	
	public boolean matchRules(Message msg){
		
		if (this.src!=null){
			if (!this.src.equals(msg.src))
				return false;
		}
		if (this.dest!=null){
			if (!this.dest.equals(msg.dest))
				return false;
		}
		if (this.kind!=null){
			if (!this.kind.equals(msg.kind))
				return false;
		}
		if (this.seqNum>=0){
			if (this.seqNum!=msg.seq)
				return false;
		}
		if (this.duplicate!=null){
			if (!this.duplicate.equals(msg.dupe))
				return false;
		}
		return true;
	}
	
}
