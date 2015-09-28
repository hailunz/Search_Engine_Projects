/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	
	public double ctf;
	public double C;
	public String field;

  /**
   *  Construct a new SCORE operator.  The SCORE operator accepts just
   *  one argument.
   *  @param q The query operator argument.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore(Qryop q) {
    this.args.add(q);
  }

  /**
   *  Construct a new SCORE operator.  Allow a SCORE operator to be
   *  created with no arguments.  This simplifies the design of some
   *  query parsing architectures.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore() {
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param q The query argument to append.
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluate the query operator.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
    	return (evaluateBoolean (r));
    if (r instanceof RetrievalModelRankedBoolean)
        return (evaluateBooleanRanked (r));
    if (r instanceof RetrievalModelBM25)
        return (evaluateBM25 ((RetrievalModelBM25) r));
    if (r instanceof RetrievalModelIndri)
        return (evaluateIndri ((RetrievalModelIndri) r));
    return null;
  }

 /**
   *  Evaluate the query operator for boolean retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    // Evaluate the query argument.

    QryResult result = args.get(0).evaluate(r);

    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.

    for (int i = 0; i < result.invertedList.df; i++) {

      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY. 
      // Unranked Boolean. All matching documents get a score of 1.0.

      result.docScores.add(result.invertedList.postings.get(i).docid,
			   (float) 1.0);
    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
	result.invertedList = new InvList();

    return result;
  }
  
  //for Ranked model
  public QryResult evaluateBooleanRanked(RetrievalModel r) throws IOException {

	    // Evaluate the query argument.

	    QryResult result = args.get(0).evaluate(r);

	    // Each pass of the loop computes a score for one document. Note:
	    // If the evaluate operation above returned a score list (which is
	    // very possible), this loop gets skipped.

	    for (int i = 0; i < result.invertedList.df; i++) {

	      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY. 
	      // Unranked Boolean. All matching documents get a score of 1.0.

	      result.docScores.add(result.invertedList.postings.get(i).docid,
	    		  result.invertedList.postings.get(i).tf);
	    }

	    // The SCORE operator should not return a populated inverted list.
	    // If there is one, replace it with an empty inverted list.

	    if (result.invertedList.df > 0)
		result.invertedList = new InvList();

	    return result;
	  }
  
  
  	//for BM25
  public QryResult evaluateBM25(RetrievalModelBM25 r) throws IOException {

	    // Evaluate the query argument.

	    QryResult result = args.get(0).evaluate(r);
	    
	    DocLengthStore dls = QryEval.dls;
	    String field=result.invertedList.field;
	    double N = QryEval.N;
	    long doclen;

	    double avg_doclen= QryEval.READER.getSumTotalTermFreq(field) /(double)QryEval.READER.getDocCount(field) ;
	    double rsj= Math.log((N-result.invertedList.df+0.5)/(result.invertedList.df+0.5));
	    double score=0;
	    int df=result.invertedList.df;
	    // Each pass of the loop computes a score for one document. Note:
	    // If the evaluate operation above returned a score list (which is
	    // very possible), this loop gets skipped.

	    for (int i = 0; i < df; i++) {

	    	doclen = dls.getDocLength(field, result.invertedList.postings.get(i).docid);
	    
	    	score= rsj* result.invertedList.postings.get(i).tf/
	    			(result.invertedList.postings.get(i).tf+r.k1*((1.0-r.b)+r.b*doclen/avg_doclen));
	    	
	    	score=Math.max(0.0,score);
	    		result.docScores.add(result.invertedList.postings.get(i).docid,
	    		  score);
	    }

	    // The SCORE operator should not return a populated inverted list.
	    // If there is one, replace it with an empty inverted list.

	    if (df > 0)
		result.invertedList = new InvList();

	    return result;
	    
	   
	  }

  public QryResult evaluateIndri(RetrievalModelIndri r) throws IOException {
		
	  	QryResult result = args.get(0).evaluate(r);
	  
	  	double score=0;
	  	double lam = r.lambda;
	  	double mu = r.mu;
	  	this.field=result.invertedList.field;
	  	this.C=(double)QryEval.corpus.get(field);
	  	this.ctf= (double)result.invertedList.ctf; 
	  
	  	long doclen ;
	  	InvList.DocPosting docp;
	  	double Pmle = this.ctf/this.C;
	
	    for (int i = 0; i < result.invertedList.df; i++) {
	    	docp=result.invertedList.postings.get(i);
	    	doclen = QryEval.dls.getDocLength(this.field, docp.docid);
	    	
	    	score = (1.0-lam)*((double)docp.tf+mu*Pmle)/((double )doclen+mu)+lam*Pmle;
	     
	    	result.docScores.add(docp.docid, score);
	    }

	    // The SCORE operator should not return a populated inverted list.
	    // If there is one, replace it with an empty inverted list.

	    if (result.invertedList.df > 0)
	    	result.invertedList = new InvList();

	    return result;
  }
  /*
   *  Calculate the default score for a document that does not match
   *  the query argument.  This score is 0 for many retrieval models,
   *  but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

	 if (r instanceof RetrievalModelIndri){
	    	double score=0;
	    	double lam = ((RetrievalModelIndri) r).lambda;
	    	double mu = ((RetrievalModelIndri) r).mu;	    	    	
		    double Pmle =this.ctf/this.C;
		
		    score = (1.0-lam)*(mu*Pmle)/(QryEval.dls.getDocLength(this.field, (int )docid)+mu)+lam*Pmle;
		    
	    	return score;
	    }
	  
    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);
    if (r instanceof RetrievalModelRankedBoolean)
        return (0.0);
    if (r instanceof RetrievalModelBM25)
        return (0.0);
   
        
    return 0.0;
  }

  /**
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#SCORE( " + result + ")");
  }
}