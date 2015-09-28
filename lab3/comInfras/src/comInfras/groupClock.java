package comInfras;

import java.util.ArrayList;

public class groupClock extends clockService {

	public int size;
	// initialize 
	public groupClock(timeStamp t, int n){
		this.ts=t;
		for (int i=0;i<n;i++){
				this.ts.vector.add(0);
		}
		this.size=n;
	}
	
	@Override
	public void updateClock() {
		synchronized(this){
			int tmp = this.ts.vector.get(this.ts.id);
			this.ts.vector.set(this.ts.id, tmp+1);
		}
	}

	public ArrayList<Integer> getTimeStamp(){
		return this.ts.vector;
	}
	
	@Override
	public void displayClock() {
		System.out.println("Current vector Clocks: " + this.ts.vector );
		
	}

	@Override
	public clockService getClock() {
		return null;
	}

	/*
	 * setClock
	 * used when generate new event
	 * this: my global clock
	 * c: clock of the event
	 * @see comInfras.clockService#setClock(comInfras.clockService)
	 */
	@Override
	public void setClock(timeStamp c) {
		synchronized (this){
			synchronized(c){
				c.vector.addAll(this.ts.vector);
				int tmp = this.ts.vector.get(this.ts.id);
				//set next clocks
				this.ts.vector.set(this.ts.id,tmp+1);
			}
		}
		
	}

	/*
	 * used when receiving messages.
	 * this: my global clock
	 * c: clock of the message
	 */
	
	@Override
	public void compareClock(timeStamp c) {
		synchronized(this){
			synchronized(c){
				// received clock is the clock of the sending msg
				int maxStamp;
				for (int i=0;i<this.size;i++){
						maxStamp= Math.max(c.vector.get(i), this.ts.vector.get(i));
						c.vector.set(i, maxStamp);
						this.ts.vector.set(i, maxStamp);
				}
				// next clock
				int tmp = this.ts.vector.get(this.ts.id);
				this.ts.vector.set(this.ts.id, tmp+1);
			}
		}
	}
	
	public boolean checkCO(timeStamp c){
		int senderId=c.id;
		boolean sendFlag = c.vector.get(senderId)==(this.ts.vector.get(senderId)+1);
		if (sendFlag){
	
			for (int i=0;i<this.size;i++){
				if (i!=senderId){
					if (c.vector.get(i)>this.ts.vector.get(i))
						return false;
				}
			}
			return true;
		}
		return false;
	}
	
	//vi[j]=vi[j]+1
	public void revUpdate(timeStamp c){
		synchronized(this){
			this.ts.vector.set(c.id,c.vector.get(c.id));
		}
	}
	
	public void sendUpdate(){
		synchronized(this){
			int tmp=this.ts.vector.get(this.ts.id);
			this.ts.vector.set(this.ts.id, tmp+1);
		}
	}
}
