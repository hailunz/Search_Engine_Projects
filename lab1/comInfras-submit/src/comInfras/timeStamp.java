package comInfras;

import java.io.Serializable;
import java.util.ArrayList;

public class timeStamp implements Serializable  {
	
	public int id;
	public int logical;
	public ArrayList<Integer> vector;
	
	public timeStamp(){
		this.id=-1;
		this.logical=-1;
		this.vector=new ArrayList<Integer>();
	}
	public timeStamp (timeStamp t){
		this.id=t.id;
		this.logical=t.logical;
		this.vector=t.vector;
	}
	public String toString(){
		String res;
		if (this.logical==-1){
		res = "Time Stamp:" + vector;
		
		}else
			res="Time Stamp:" + logical;
		return res;
			
	}
	// compare a logical timeStamp
		public String compareLogical(timeStamp a){
			String res="";
			if (this.logical == a.logical)
				res="concurrent";
			else if (this.logical < a.logical)
				res="less";
			else{
				res = "greater";
			}
			return res;
		}
		
		// compare a vector time Stamp
		public String compareVector(timeStamp a){
			String res="";
			boolean equ=true;
			boolean less=false;
			boolean greater=false;
			
			
			//check equal
			for (int i=0;i<this.vector.size();i++){
				if (!this.vector.get(i).equals(a.vector.get(i))){
					equ=false;
					break;
				}
			}
			if (equ){
				res="equal";
				return res;
			}
			int com;
			//this <= a
			for (int i=0;i<this.vector.size();i++){
				com=this.vector.get(i).compareTo(a.vector.get(i));
				if (com==0)
					continue;
				if (com<0){
					if (greater){
						res="concurrent";
						return res;
					}
					less=true;
				}else{
					if (less){
						res="concurrent";
						return res;
					}
					
					greater = true;
				}
				
			}
			if (greater){		
				res="greater";
			}
			if (less)
				res="less";
			
			return res;
		}
	
}
