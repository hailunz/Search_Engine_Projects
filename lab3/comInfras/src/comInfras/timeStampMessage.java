package comInfras;

public class timeStampMessage extends Message {
	
	public timeStamp timeStamp;
	public String oriSrc;
	public String oriType;
	public String mGroup;
	public timeStamp gtimeStamp;
	public String reqHost;
	
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
		this.gtimeStamp= new timeStamp();
		this.oriSrc=m.src;
		this.mGroup=null;
		this.reqHost=null;
		this.oriType=this.kind;
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
		tsMsg.gtimeStamp=this.gtimeStamp;
		tsMsg.oriSrc=this.oriSrc;
		tsMsg.mGroup=this.mGroup;
		tsMsg.oriType= this.oriType;
		tsMsg.reqHost = this.reqHost;
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
		  str.append(" group time stamp:"+this.gtimeStamp.toString());
		  str.append(" group:"+this.mGroup);
		  str.append(" oriSrc:"+this.oriSrc);
		  str.append(" oriType:"+this.oriType);
		  str.append(" reqHost:"+this.reqHost);

		  return str.toString();
	}
	public void displayMessage(){
		System.out.println(this.toString()+this.timeStamp.toString());
	}
	

	public void clone(timeStampMessage m){
		this.src=m.src;
		this.data=m.data;
		this.kind=m.kind;
		this.dest=m.dest;
		this.dupe=m.dupe;
		this.seq=m.seq;
		this.timeStamp=new timeStamp(m.timeStamp);
		this.gtimeStamp=new timeStamp(m.gtimeStamp);
		this.oriSrc=m.oriSrc;
		this.mGroup=m.mGroup;
		this.oriType=m.oriType;
		this.reqHost= m.reqHost;
	}
	public String toMulti(){
		StringBuilder str = new StringBuilder();
		str.append("oriSrc:"+this.oriSrc);
		str.append("seq:"+this.seq);
		return str.toString();
	}
	
}
