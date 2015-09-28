import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class QryopIlWindow extends QryopIl{

	public int distance;
	
	public QryopIlWindow(int dis,Qryop... q) {
	    for (int i = 0; i < q.length; i++)
	      this.args.add(q[i]);
	    this.distance=dis;
	}
	
	 public QryopIlWindow(Qryop... q) {
		    for (int i = 0; i < q.length; i++)
		      this.args.add(q[i]);
		  }
	@Override
	public void add(Qryop q) throws IOException {
		 this.args.add(q);
		
	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		 //  Initialization

	    allocArgPtrs (r);  
	    syntaxCheckArgResults (this.argPtrs);
	    
	    QryResult result = new QryResult ();
	    result.invertedList.field = new String (this.argPtrs.get(0).invList.field);
	   
	    int minPos,maxPos;
	    int minIndex=0;
	    
	    ArgPtr ptr0 = this.argPtrs.get(0);
	    int []posPointer= new int[this.argPtrs.size()];
	    Arrays.fill(posPointer, 0);
	    int size = this.argPtrs.size();
	  
	    EVALUATEDOCUMENTS:
	        for ( ; ptr0.nextDoc < ptr0.invList.df; ptr0.nextDoc ++) {
	        	Arrays.fill(posPointer, 0);
	          int ptr0Docid = ptr0.invList.getDocid (ptr0.nextDoc);

	          for (int j=1; j<size; j++) {

	        	  ArgPtr ptrj = this.argPtrs.get(j);

	        	  while (true) {
	        		  if (ptrj.nextDoc >= ptrj.invList.df)
	        			  break EVALUATEDOCUMENTS;		// No more docs can match
	        		  else if (ptrj.invList.getDocid (ptrj.nextDoc) > ptr0Docid)
	        				  continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
	        		else if (ptrj.invList.getDocid (ptrj.nextDoc) < ptr0Docid)
	        					  ptrj.nextDoc ++;			// Not yet at the right doc.
	        		else{
	        			break;				// ptrj matches ptr0Docid
	        		}
	    		  
	        	  }	
	          }
	          
	          // all args match doc id
	          List<Integer> storePositions = new ArrayList<Integer>();
	          int tf = ptr0.invList.postings.get(ptr0.nextDoc).tf;
	          
	          EvaluatePositions:
	        	  while(posPointer[0]<tf){
	        		  minPos=  ptr0.invList.postings.get(ptr0.nextDoc).positions.get(posPointer[0]);
	   	           maxPos=minPos;
	   	           minIndex=0;
	        		  for (int j=1; j<this.argPtrs.size(); j++) {

	        			  ArgPtr ptrj = this.argPtrs.get(j);
		        	  
		        		  if (posPointer[j] >= ptrj.invList.postings.get(ptrj.nextDoc).tf)
		        			  break EvaluatePositions;		// No more locs can match
		        		  else{
		        			  int curPos = ptrj.invList.postings.get(ptrj.nextDoc).positions.get(posPointer[j]);
			        			if (minPos > curPos){
			        				minPos=curPos;
			        				minIndex=j;
			        			}
			        			if (maxPos<curPos){
			        				maxPos=curPos;
			        			}
		        		  }
		        	  }
	        		  if ((maxPos-minPos)<this.distance){// match add to the result list.
	    	        	  storePositions.add(maxPos);
	    	        	  // advance all the loc pointers;
	    	        	  for (int j=0;j<size;j++){
	    	        		  posPointer[j]++;
	    	        	  }
	    	          }
	    	          else{
	    	        	  posPointer[minIndex]++;
	    	          }
	        	  
		          }
	          
	          	if (storePositions.size()>0){
	          		result.invertedList.appendPosting(ptr0Docid, storePositions);
	          	}
	          
	          }
	          
	    	freeArgPtrs ();
	    	return result;
	}
	
	
	 /**
	   *  syntaxCheckArgResults does syntax checking that can only be done
	   *  after query arguments are evaluated.
	   *  @param ptrs A list of ArgPtrs for this query operator.
	   *  @return True if the syntax is valid, false otherwise.
	   */
	  public Boolean syntaxCheckArgResults (List<ArgPtr> ptrs) {

	    for (int i=0; i<this.args.size(); i++) {

	      if (! (this.args.get(i) instanceof QryopIl)) 
		QryEval.fatalError ("Error:  Invalid argument in " +
				    this.toString());
	      else
		if ((i>0) &&
		    (! ptrs.get(i).invList.field.equals (ptrs.get(0).invList.field)))
		  QryEval.fatalError ("Error:  Arguments must be in the same field:  " +
				      this.toString());
	    }

	    return true;
	  }
	@Override
	public String toString() {
		String result = new String ();
	    for (int i=0; i<this.args.size(); i++)
	      result += this.args.get(i).toString() + " ";
	    return ("#Window/"+this.distance+"( " + result + ")");
	}

}
