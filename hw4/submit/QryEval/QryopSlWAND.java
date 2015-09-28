import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class QryopSlWAND extends QryopSl{

	public List<Double> weight;
	public List<Double> order;
	public int count;
	
	public QryopSlWAND(Qryop... q) {
	    for (int i = 0; i < q.length; i++)
	      this.args.add(q[i]);
	    this.weight= new ArrayList<Double>();
	    this.order= new ArrayList<Double>();
	  }
	
	public void parse(){
		
	}
	
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		 if (r instanceof RetrievalModelIndri){
			  double w=0;
			  for (int i=0;i<this.args.size();i++){
				  w+= this.weight.get(i);
			  }

			  for (int i=0;i<this.args.size();i++){
				  this.order.add(this.weight.get(i)/w);
			  }
			  
		    	double score=1.0;
		    	for (int i=0;i<this.args.size();i++){
		    		score *=Math.pow(((QryopSl)args.get(i)).getDefaultScore(r, docid), this.order.get(i));
		    	}
		    	return score;
		    }
		    
		 return 0.0;
	}

	@Override
	public void add(Qryop q) throws IOException {
		this.args.add(q);
		
	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		  allocArgPtrs (r);
		  QryResult result = new QryResult ();
		  
		  if (this.argPtrs.size()<=0){
			  freeArgPtrs ();
			  return result;
		  }
		  
		  ArgPtr ptr0 = this.argPtrs.get(0);

		  double w=0;
		  for (int i=0;i<this.argPtrs.size();i++){
			  w+= this.weight.get(i);
		  }
		  for (int i=0;i<this.argPtrs.size();i++){
			  this.order.add(this.weight.get(i)/w);
		  }
		  while (true) {

		      int nextDocid = getSmallestCurrentDocid ();

		      // no more docs to be count
		      	if (nextDocid == Integer.MAX_VALUE)
		      		break;
		      	
		      //  Create a new posting that is the union of the posting lists
		      //  that match the nextDocid.

		      double Score = 1.0;
		      
		     
		      for (int i=0; i<this.argPtrs.size();i++) {
		    	  ArgPtr ptri = this.argPtrs.get(i);

		    	  if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
		    		  Score *=Math.pow(((QryopSl)this.args.get(i)).getDefaultScore(r, nextDocid), this.order.get(i)); 
		    		  
		    	  }
		    	  else if (ptri.scoreList.getDocid (ptri.nextDoc) == nextDocid) {
		    		  Score *= Math.pow(ptri.scoreList.getDocidScore(ptri.nextDoc), this.order.get(i));
		    		  ptri.nextDoc ++;
		    		
		    	  }else{
		    		  Score *=Math.pow(((QryopSl)this.args.get(i)).getDefaultScore(r, nextDocid), this.order.get(i));
		    	  }
		      }
		      
		      	result.docScores.add (nextDocid, Score);
		    }
		  
		    
		  freeArgPtrs ();
		  return result;
	}
	
	 public int getSmallestCurrentDocid () {

		    int nextDocid = Integer.MAX_VALUE;

		    for (int i=0; i<this.argPtrs.size(); i++) {
		      ArgPtr ptri = this.argPtrs.get(i);
		       
		      if (ptri.nextDoc>= ptri.scoreList.scores.size())
		    	  continue;
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

	    return ("#WAND( " + result + ")");
	}

}
