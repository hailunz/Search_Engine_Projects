package comInfras;

public class logicalClock extends clockService {

	// initialize 
	public logicalClock(timeStamp t){
		this.ts=t;
		this.ts.logical=0;
	}
	
	@Override
	public void displayClock() {
		System.out.println("LogicalClock: "+ this.ts.logical);
	}

	@Override
	public clockService getClock() {
		return this;
	}
	/*
	 * setClock
	 * set the clock for my events 
	 * this: my global clock
	 * c: clock of the event
	 * @see comInfras.clockService#setClock(comInfras.clockService)
	 */
	@Override
	public void setClock(timeStamp c) {
		synchronized (this){
			synchronized(c){
				c.logical=this.ts.logical;
				this.ts.logical++;
			}
		}
		
	}

	/*
	 * used for global clock.
	 */
	@Override
	public void updateClock() {	
		synchronized(this){
			this.ts.logical++;
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
				c.logical++;
				if (c.logical>this.ts.logical){
					this.ts.logical=c.logical;
				}else{
					c.logical=this.ts.logical;
				}
				this.ts.logical++;
			}
		}
	}

}
