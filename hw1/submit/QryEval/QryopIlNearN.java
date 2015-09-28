import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QryopIlNearN extends QryopIl{
	
	public int distance;
	
	public QryopIlNearN(int dis,Qryop... q) {
	    for (int i = 0; i < q.length; i++)
	      this.args.add(q[i]);
	    this.distance=dis;
	  }
	
	  /**
	   *  It is convenient for the constructor to accept a variable number
	   *  of arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	   */
	  public QryopIlNearN(Qryop... q) {
	    for (int i = 0; i < q.length; i++)
	      this.args.add(q[i]);
	  }

	  /**
	   *  Appends an argument to the list of query operator arguments.  This
	   *  simplifies the design of some query parsing architectures.
	   *  @param {q} q The query argument (query operator) to append.
	   *  @return void
	   *  @throws IOException
	   */
	  public void add (Qryop a) {
	    this.args.add(a);
	  }

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		 if (r instanceof RetrievalModelUnrankedBoolean) 
		      return (evaluateBoolean (r));
		 if (r instanceof RetrievalModelRankedBoolean)
			  return (evaluateBoolean (r));
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		
		 String result = new String ();

		    for (int i=0; i<this.args.size(); i++)
		      result += this.args.get(i).toString() + " ";
		    return ("#Near/"+this.distance+"( " + result + ")");
	}
	
	// evaluate Boolean model
	/*
	 * My solution is to store middle result tmpRes. 
	 * Each iteration, I compare two adjacent invList,for example A and B. 
	 * First find the document they match, then iterate the positions in that doc.
	 * If a position meets the distance requirement, then add B's position into tmpRes,
	 * and move the positions pointers both to the next position.
	 * After searching through all the matched docs, compare the tmpRes with the next invList.
	 * I have added the syntaxCheckArgResults method, like Syn operator does
	 */
    public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

		    //  Initialization

		    allocArgPtrs (r);  
		    syntaxCheckArgResults (this.argPtrs);
		    
		    QryResult prevRes = new QryResult ();
		    prevRes.invertedList.field = new String (this.argPtrs.get(0).invList.field);
		    
		    ArgPtr ptr = this.argPtrs.get(0);
		    prevRes.invertedList = ptr.invList;
		    QryResult tmpRes ;
		    int count = this.argPtrs.size();
		    
		    for (int i=1; i<count;i++){
	
		    	tmpRes= new QryResult();
		    	ArgPtr curPtr = this.argPtrs.get(i);
		    	
		    	// count the number of docs in prev invlist
		    	int prevDocs=0;
		    
		    	while (prevDocs<prevRes.invertedList.df && curPtr.nextDoc<curPtr.invList.df){
		    		
		    		// the prev docid and the current docid
		    		int prevDocID=prevRes.invertedList.getDocid(prevDocs);
		    		int curDocID=curPtr.invList.getDocid(curPtr.nextDoc);
		    		
		    		// if the prevDocId is bigger than current docid, 
		    		//then move the current doc to the next doc in curPtr
		    		if (prevDocID>curDocID){
		    			curPtr.nextDoc++;
		    		}else if(prevDocID==curDocID) {
		    			// same docid, then check matches of the each positions.
		    			int prevPos=0, curPos=0;
		    			InvList prevList = prevRes.invertedList;
		    			InvList curList = curPtr.invList;
		    			List<Integer> storePositions = new ArrayList<Integer>();
		    			
		    			//loop for each positions in this doc
		    			while(prevPos<prevList.postings.get(prevDocs).tf && curPos<curList.postings.get(curPtr.nextDoc).tf){
		    				if (prevList.postings.get(prevDocs).positions.get(prevPos)>curList.postings.get(curPtr.nextDoc).positions.get(curPos)){
		    					curPos++;
		    				}else {
		    					if( (prevList.postings.get(prevDocs).positions.get(prevPos)+this.distance) >= curList.postings.get(curPtr.nextDoc).positions.get(curPos)){
		    						//match
		    						storePositions.add(curList.postings.get(curPtr.nextDoc).positions.get(curPos));
		    						prevPos++;
		    						curPos++;
		    					}
		    					else{
		    						prevPos++;
		    					}
		    				}
		    			}
		    			
		    			if (storePositions.size()>0){
		    				//append
		    				tmpRes.invertedList.appendPosting(prevDocID, storePositions);
		    			}
		    			prevDocs++;
		    			curPtr.nextDoc++;
		 
		    		}else{
		    			prevDocs++;
		    		}
		    	}
		    	// have checked all the docs for these two invlist
		    	prevRes = tmpRes;
		    	prevRes.invertedList.field = new String(curPtr.invList.field);
		    }

		    freeArgPtrs ();
		    return prevRes;
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

}