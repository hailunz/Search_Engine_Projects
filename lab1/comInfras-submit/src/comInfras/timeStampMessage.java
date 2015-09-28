package comInfras;

public class timeStampMessage extends Message {
	
	public timeStamp timeStamp;
	
	public timeStampMessage(){
		
	}

	public timeStampMessage(Message m){
		this.data=m.data;
		this.kind=m.kind;
		this.dest=m.dest;
		this.dupe=m.dupe;
		this.seq=m.seq;
		this.src=m.src;
		this.timeStamp=new timeStamp();
	}
	public timeStampMessage(String dest, String kind, Object data) {
		super(dest, kind, data);
	}
	
	public void setTimeStamp(timeStamp c){
		this.timeStamp=c;
	}
	
	
	public timeStampMessage dupMsg(){
		timeStampMessage tsMsg = new timeStampMessage();
		tsMsg.data=this.data;
		tsMsg.kind=this.kind;
		tsMsg.dest=this.dest;
		tsMsg.dupe="true";
		tsMsg.seq=this.seq;
		tsMsg.src=this.src;
		tsMsg.timeStamp=this.timeStamp;
		
		return tsMsg;
	}
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
		  str.append(" payload:"+this.data.toString());
		  str.append(" time stamp:"+this.timeStamp.toString());

		  return str.toString();
	}
	public void displayMessage(){
		System.out.println(this.toString()+this.timeStamp.toString());
	}
	
}
