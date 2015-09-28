import java.io.IOException;

public class QryopSlOr extends QryopSl{

	
	/**
	   *  It is convenient for the constructor to accept a variable number
	   *  of arguments. Thus new qryopOr (arg1, arg2, arg3, ...).
	   *  @param q A query argument (a query operator).
	   */
	  public QryopSlOr(Qryop... q) {
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
	    
		return 0;
	}

	@Override
	public void add(Qryop q) throws IOException {
		this.args.add(q);
		
	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
	    if (r instanceof RetrievalModelUnrankedBoolean)
	    	return (evaluateBoolean (r));
	    if (r instanceof RetrievalModelRankedBoolean)
	      return (evaluateBooleanRanked (r));
	    
	    return null;
	}
	
	/**
	   *  Evaluates the query operator for unranked boolean retrieval models,
	   *  including any child operators and returns the result.
	   *  @param r A retrieval model that controls how the operator behaves.
	   *  @return The result of evaluating the query.
	   *  @throws IOException
	   */
	 public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		    //  Initialization

		    allocArgPtrs (r);
		    QryResult result = new QryResult ();
		    
		    ArgPtr ptr0 = this.argPtrs.get(0);

		    //  Each pass of the loop adds 1 document to result until all of
		    //  the inverted lists are depleted.  When a list is depleted, it
		    //  is removed from argPtrs, so this loop runs until argPtrs is empty.

		    while (this.argPtrs.size() > 0) {

		      int nextDocid = getSmallestCurrentDocid ();
		      // no more docs to be added
		      if (nextDocid == Integer.MAX_VALUE)
		    	  break;

		      double maxScore = 1;
		      for (int i=0; i<this.argPtrs.size(); i++) {
		    	  ArgPtr ptri = this.argPtrs.get(i);
		    	  
		    	  // point to the next Doc
		    	  if (ptri.scoreList.getDocid (ptri.nextDoc) == nextDocid) {
		    		  ptri.nextDoc ++;
		    	  }
		      }
		      result.docScores.add (nextDocid, maxScore);

		      //  If an ArgPtr has reached the end of its list, remove it.
		      //  The loop is backwards so that removing an arg does not
		      //  interfere with iteration.

		      for (int i=this.argPtrs.size()-1; i>=0; i--) {
		    	  ArgPtr ptri = this.argPtrs.get(i);

		    	  if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
		    		  this.argPtrs.remove (i);
		    	  }
		      }
		    }

		    freeArgPtrs();
		    return result;
		  }


	  
	  /**
	   *  Evaluates the query operator, including any child operators and
	   *  returns the result.
	   *  @param r A retrieval model that controls how the operator behaves.
	   *  @return The result of evaluating the query.
	   *  @throws IOException
	   */
	  public QryResult evaluateBooleanRanked(RetrievalModel r) throws IOException {

	    //  Initialization

	    allocArgPtrs (r);  
	    QryResult result = new QryResult ();
	
	    ArgPtr ptr0 = this.argPtrs.get(0);
	    
	    //if the first ptr has no scores return 
	    if (ptr0.scoreList.scores.size()<1){
	    	freeArgPtrs ();
	        return result;
	    }

	    //  Each pass of the loop adds 1 document to result until all of
	    //  the inverted lists are depleted.  When a list is depleted, it
	    //  is removed from argPtrs, so this loop runs until argPtrs is empty.

	    while (this.argPtrs.size() > 0) {

	      int nextDocid = getSmallestCurrentDocid ();

	      // no more docs to be count
	      	if (nextDocid == Integer.MAX_VALUE)
	      		break;
	      	
	      //  Create a new posting that is the union of the posting lists
	      //  that match the nextDocid.

	      double maxScore = 0;
	      for (int i=this.argPtrs.size()-1; i>=0; i--) {
	    	  ArgPtr ptri = this.argPtrs.get(i);

	    	  if (ptri.scoreList.getDocid (ptri.nextDoc) == nextDocid) {
	    		  maxScore=Math.max(maxScore, ptri.scoreList.getDocidScore(ptri.nextDoc));
	    		  ptri.nextDoc ++;
	    		  if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
		    		  this.argPtrs.remove (i);
		    	  }
	    	  }
	      }
	      	result.docScores.add (nextDocid, maxScore);
	    }
	    freeArgPtrs();
	    return result;
	  }

	  /**
	   *  Return the smallest unexamined docid from the ArgPtrs.
	   *  @return The smallest internal document id.
	   */
	  public int getSmallestCurrentDocid () {

	    int nextDocid = Integer.MAX_VALUE;

	    for (int i=0; i<this.argPtrs.size(); i++) {
	      ArgPtr ptri = this.argPtrs.get(i);
	      
	      //if the ptri scores is null 
	      if (ptri.scoreList.scores.size()<1){
	    	  this.argPtrs.remove(i);
	    	  i--;
	    	  continue;
	      }
	      
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
		    return ("#OR( " + result + ")");
	}

}