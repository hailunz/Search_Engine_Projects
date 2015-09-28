package comInfras;

import java.io.Serializable;

/*
 * 18842 lab0
 * team 32
 * Hailun Zhu, ID: hailunz; 
 */

public class Message implements Serializable {
	
		// head variables
	 	
		//destination node
		public String src;
		public String dest;
		
		//sequence number
		public int seq;
		
		//duplicate flag
		public String dupe;
		
		//message kind
		public String kind;
		
		//payload
		public Object data;
		public Message(){
			
		}
	
		public Message(String dest, String kind, Object data) {
			this.dest=dest;
			this.kind=kind;
			this.data=data;
		}
		
	  // These settors are used by MessagePasser.send, not your app
		
	public void set_dest(String dest) {
			  this.dest=dest;
			  
	}
	  public void set_source(String source) {
		  this.src=source;
		  
	}
	  public void set_kind(String kind) {
		  this.kind=kind;
		  }
	  
	  public void set_seqNum(int sequenceNumber) {
		  this.seq=sequenceNumber;
	}
	  public void set_duplicate(String dup) {
			  this.dupe="false";	 
	}
	  public void set_data(Object data) {
			  this.data=data;	 
	}
	  public Message dupMsg(){
		  Message res = new Message(this.dest,this.kind,this.data);
		  res.src=this.src;
		  res.seq=this.seq;
		  res.dupe="true";
		  return res;
	  }
	 // other accessors, toString, etc as needed
	  public String toString(){
		  StringBuilder str = new StringBuilder();
		  str.append("src:"+this.src+" dest:"+this.dest);
		  str.append(" seqNum:"+this.seq);
		  str.append(" kind:"+this.kind+" dupe:");
		  if (this.dupe.equals("true")){
			  str.append("true");
			}else{
				str.append("false");
			}
		  str.append("\npayload:"+this.data.toString());
		  return str.toString();
	  }
}
