import java.io.IOException;

public class QryopSlSum extends QryopSl {

	
	/**
	   *  It is convenient for the constructor to accept a variable number
	   *  of arguments. Thus new qryopSum (arg1, arg2, arg3, ...).
	   *  @param q A query argument (a query operator).
	   */
	  public QryopSlSum(Qryop... q) {
	    for (int i = 0; i < q.length; i++)
	      this.args.add(q[i]);
	  }
	
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		 if (r instanceof RetrievalModelUnrankedBoolean)
		    	return (0.0);
		 if (r instanceof RetrievalModelRankedBoolean)
		        return (0.0);
		 if (r instanceof RetrievalModelBM25)
		        return (0.0);
		return 0;
	}

	public QryResult evaluateBM25(RetrievalModelBM25 r) throws IOException{
		
		 //  Initialization
	    allocArgPtrs (r);  
	    QryResult result = new QryResult ();
	    
	    while (this.argPtrs.size() > 0) {

		      int nextDocid = getSmallestCurrentDocid ();

		      // no more docs to be count
		      	if (nextDocid == Integer.MAX_VALUE)
		      		break;
		      	
		      //  Create a new posting that is the union of the posting lists
		      //  that match the nextDocid.

		      double sumScore = 0;
		      for (int i=this.argPtrs.size()-1; i>=0; i--) {
		    	  ArgPtr ptri = this.argPtrs.get(i);
		    	 
		    	  if (ptri.scoreList.getDocid (ptri.nextDoc) == nextDocid) {
		    		  sumScore += ptri.scoreList.getDocidScore(ptri.nextDoc);
		    		  ptri.nextDoc ++;
		    		  if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
			    		  this.argPtrs.remove (i);
			    	  }
		    	  }
		      }
		      	result.docScores.add (nextDocid, sumScore);
		    }
	    
	    
	    freeArgPtrs();
		return result;
	}
	@Override
	public void add(Qryop q) throws IOException {
		this.args.add(q);

	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		
		if (r instanceof RetrievalModelBM25)
		        return (evaluateBM25 ((RetrievalModelBM25) r));
		return null;
	}
	
	 /**
	   *  Return the smallest unexamined docid from the ArgPtrs.
	   *  @return The smallest internal document id.
	   */
	  public int getSmallestCurrentDocid () {

	    int nextDocid = Integer.MAX_VALUE;

	    for (int i=0; i<this.argPtrs.size(); i++) {
	      ArgPtr ptri = this.argPtrs.get(i);
	      
//	      //if the ptri scores is null 
//	      if (ptri.scoreList.scores.size()<1){
//	    	  this.argPtrs.remove(i);
//	    	  i--;
//	    	  continue;
//	      }
	      
	      if (nextDocid >  ptri.scoreList.getDocid (ptri.nextDoc))
	    	  nextDocid = ptri.scoreList.getDocid (ptri.nextDoc);
	     }

	    return (nextDocid);
	  }

	@Override
	public String toString() {
		 String result = new String ();

		    for (int i=0; i<this.args.size(); i++)
		      result += this.args.get(i).toString() + " ";
		    
		 return ("#SUM (" + result + ")");
	
	}

}
