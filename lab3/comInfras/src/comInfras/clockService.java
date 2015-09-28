package comInfras;

import java.util.ArrayList;

public abstract class clockService {
	
	public timeStamp ts;
	
	public abstract void displayClock();
	public abstract clockService getClock();
	public abstract void setClock (timeStamp c);
	public abstract void updateClock();
	public abstract void compareClock(timeStamp c);
	
}
