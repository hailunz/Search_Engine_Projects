/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class QryEval {
	
  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  //  The index file reader is accessible via a global variable. This
  //  isn't great programming style, but the alternative is for every
  //  query operator to store or pass this value, which creates its
  //  own headaches.

  public static IndexReader READER;
  public static DocLengthStore dls;
  public static HashMap<String, Long> corpus;
  public static double N;
  //  Create and configure an English analyzer that will be used for
  //  query parsing.

  public static EnglishAnalyzerConfigurable analyzer =
      new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }
  
  
 
  /**
   *  @param args The only argument is the path to the parameter file.
   *  @throws Exception
   */
  public static void main(String[] args) throws Exception {
    
	  long time1=System.currentTimeMillis();
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();
    
    // parameters required for this program to run
    if (!params.containsKey("queryFilePath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }
    if (!params.containsKey("indexPath")) {
        System.err.println("Error: Parameters were missing.");
        System.exit(1);
      }
    if (!params.containsKey("trecEvalOutputPath")) {
        System.err.println("Error: Parameters were missing.");
        System.exit(1);
      }
    if (!params.containsKey("retrievalAlgorithm")) {
        System.err.println("Error: Parameters were missing.");
        System.exit(1);
      }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    
    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    N= (double)QryEval.READER.numDocs();
   // DocLengthStore s = new DocLengthStore(READER);
    dls = new DocLengthStore(QryEval.READER);
    corpus = new HashMap<String,Long>();
    corpus.put("body", QryEval.READER.getSumTotalTermFreq("body"));
    corpus.put("url", QryEval.READER.getSumTotalTermFreq("url"));
    corpus.put("inlink", QryEval.READER.getSumTotalTermFreq("inlink"));
    corpus.put("keywords", QryEval.READER.getSumTotalTermFreq("keywords"));
    corpus.put("title", QryEval.READER.getSumTotalTermFreq("title"));
    
    /*
     * set the retrieval model
     */
    String modelKind = params.get("retrievalAlgorithm");

    RetrievalModel model=null;
    RetrievalModelBM25 m = new RetrievalModelBM25();

    if (modelKind.equals("UnrankedBoolean"))
    		model = new RetrievalModelUnrankedBoolean();
    else if (modelKind.equals("RankedBoolean"))
    		model = new RetrievalModelRankedBoolean();
    
    else if (modelKind.equals("BM25")){
    	model = new RetrievalModelBM25();
    	 if (!params.containsKey("BM25:k_1")) {
    	        System.err.println("Error: Parameters were missing.");
    	        System.exit(1);
    	      }
    	 if (!params.containsKey("BM25:b")) {
    	        System.err.println("Error: Parameters were missing.");
    	        System.exit(1);
    	      }
    	 if (!params.containsKey("BM25:k_3")) {
    	        System.err.println("Error: Parameters were missing.");
    	        System.exit(1);
    	      }
    	 double k1= Double.parseDouble(params.get("BM25:k_1"));
    	 double b = Double.parseDouble(params.get("BM25:b"));
    	 double k3 = Double.parseDouble(params.get("BM25:k_3"));
    	boolean flag= model.setParameter("k1", k1) && model.setParameter("b", b) && model.setParameter("k3", k3) ;

    	if (!flag){
    		 System.err.println("Error:BM25 Set Parameters Wrong.");
    	     System.exit(1);
    	}
    }
    		
    else if (modelKind.equals("Indri")){
    	model = new RetrievalModelIndri();
   	 if (!params.containsKey("Indri:mu")) {
	        System.err.println("Error: Parameters were missing.");
	        System.exit(1);
	      }
	 if (!params.containsKey("Indri:lambda")) {
	        System.err.println("Error: Parameters were missing.");
	        System.exit(1);
	      }
    	double mu =Double.parseDouble(params.get("Indri:mu"));
        double lambda =  Double.parseDouble(params.get("Indri:lambda"));
    	boolean flag=model.setParameter("mu", mu) && model.setParameter("lambda", lambda);
    	if (!flag){
   		 	System.err.println("Error:Indri Set Parameters Wrong.");
   		 	System.exit(1);
    	}
    }
    
    else if (modelKind.equals("letor")){
      model = new RetrievalModelLeToR();

      double k1= Double.parseDouble(params.get("BM25:k_1"));
      double b = Double.parseDouble(params.get("BM25:b"));
      double k3 = Double.parseDouble(params.get("BM25:k_3"));
      double mu =Double.parseDouble(params.get("Indri:mu"));
      double lambda =  Double.parseDouble(params.get("Indri:lambda"));
      m.setParameter("k1", k1);
      m.setParameter("k3", k3);
      m.setParameter("b", b);
      boolean flag= model.setParameter("k1", k1) && model.setParameter("b", b) && model.setParameter("k3", k3) ;
      flag = flag && model.setParameter("mu", mu) && model.setParameter("lambda", lambda);

      if (!flag){
        System.err.println("Error:BM25 Set Parameters Wrong.");
        System.exit(1);
      }
    }
 
//    
    /**
     *  The index is open. Start evaluating queries. The examples
     *  below show query trees for two simple queries.  These are
     *  meant to illustrate how query nodes are created and connected.
     *  However your software will not create queries like this.  Your
     *  software will use a query parser.  See parseQuery.
     *
     *  The general pattern is to tokenize the  query term (so that it
     *  gets converted to lowercase, stopped, stemmed, etc), create a
     *  Term node to fetch the inverted list, create a Score node to
     *  convert an inverted list to a score list, evaluate the query,
     *  and print results.
     * 
     *  Modify the software so that you read a query from a file,
     *  parse it, and form the query tree automatically.
     */

    // for hw4 fb 
    boolean fb = params.containsKey("fb")? (!params.get("fb").equals("false")) : false;  
    String fbRankingFile = null;
    if (params.containsKey("fbInitialRankingFile")){
    	fbRankingFile = params.get("fbInitialRankingFile");
    }
    int fbDocs=0,fbTerms=0;
    double fbMu=0;
    double fbOrigWeight =0;
    String fbExpanQueryFile=null;
    BufferedWriter writer1 = null;

    // get fb parameters
    if(fb){
    	 if (! (params.containsKey("fbDocs") && params.containsKey("fbTerms") && params.containsKey("fbMu") 
    			 && params.containsKey("fbOrigWeight") && params.containsKey("fbExpansionQueryFile"))) {
 	        System.err.println("Error: Parameters were missing.");
 	        System.exit(1);
 	      }
    	
    	fbDocs= Integer.parseInt(params.get("fbDocs"));
    	fbTerms = Integer.parseInt(params.get("fbTerms"));
    	fbOrigWeight = Double.parseDouble(params.get("fbOrigWeight"));
    	fbMu = Double.parseDouble(params.get("fbMu"));
    	fbExpanQueryFile = params.get("fbExpansionQueryFile");
    	writer1 = new BufferedWriter(new FileWriter(new File(fbExpanQueryFile)));
    	
    }
    
    HashMap <String, HashMap<Integer, Double>>fileMap = new HashMap<String, HashMap<Integer, Double>>();
    
    // read from fbRankingFile
    if( fb && fbRankingFile !=null) {
    	Scanner scanFile = new Scanner (new File(fbRankingFile));
    	
    	// get the first line 
    	String sLine = scanFile.nextLine();
		
    	while(scanFile.hasNext()){
    		//get one query's score
    		String []tokens = sLine.split("\\s+");
    		HashMap<Integer, Double> qMap = new HashMap<Integer,Double>();
    		int internalId = getInternalDocid(tokens[2]);
    		double score = Double.parseDouble(tokens[4]);
    		qMap.put(internalId, score);
    		for (int i=1;i<fbDocs;i++){
    			 sLine = scanFile.nextLine();
    			 tokens = sLine.split("\\s+");
        		 internalId = getInternalDocid(tokens[2]);
        		 score = Double.parseDouble(tokens[4]);
        		qMap.put(internalId, score);
    		}
    		fileMap.put(tokens[0], qMap);
    		
    		while (scanFile.hasNext() && (sLine=scanFile.nextLine()).startsWith(tokens[0]))
    			continue;
    		
    		// fbDocs==1
//    		if (!scanFile.hasNext()){
//    			tokens = sLine.split("\\s+");
//        		qMap = new HashMap<Integer,Double>();
//        		internalId = getInternalDocid(tokens[2]);
//        		score = Double.parseDouble(tokens[4]);
//        		qMap.put(internalId, score);
//        		fileMap.put(tokens[0], qMap);
//    		}
    	}
    	
    	scanFile.close();
    }
    
    
    // read from trainingQrelsFile qid <external-docId, relevance >
    HashMap <String, HashMap<String, String>> relFileMap = new HashMap<String, HashMap<String, String>>();
    HashMap <String, Float > pageRankMap = new HashMap<String, Float> ();
    boolean [] disable = new boolean[18];
    
    if( modelKind.equals("letor")) {
      if (params.get("letor:trainingQrelsFile") !=null ){
        Scanner scanFile = new Scanner (new File((params.get("letor:trainingQrelsFile"))));

        String sLine;
        sLine = scanFile.nextLine();
        while(scanFile.hasNext()){
          //get one query's score
          String []tokens = sLine.split("\\s+");
          HashMap<String, String> qMap = new HashMap<String, String>();
          qMap.put(tokens[2], tokens[3]);
          String qid = tokens[0];
          while (scanFile.hasNext() && (sLine=scanFile.nextLine()).startsWith(qid+" ")){
            tokens = sLine.split("\\s+");
            qMap.put(tokens[2], tokens[3]);
          }
          relFileMap.put(qid,qMap);
          
        }
        scanFile.close();
      }else{
        System.err.println("Error: Parameters were missing.");
        System.exit(1);
      }
      if (params.get("letor:pageRankFile") != null){
        Scanner scanFile = new Scanner (new File(params.get("letor:pageRankFile")));
       
        String []lines = null;
        int count=0;
        while (scanFile.hasNext()){
          lines= scanFile.nextLine().split("\\s+");
          if (pageRankMap.containsKey(lines[0])){
        	  continue;
          }
          
          pageRankMap.put(lines[0],Float.parseFloat(lines[1]));
        }
       
        scanFile.close();
      }else{
        System.err.println("Error: Parameters were missing.");
        System.exit(1);
      }
      
      if(params.get("letor:featureDisable")!=null){
    	  String []dis = params.get("letor:featureDisable").split(",");
    	  int tmp;
    	  for (int i=0;i<dis.length;i++){
    		  tmp = Integer.parseInt(dis[i]);
    		  disable[tmp-1]=true;
    	  }
    	  
      }

    }

    float avg_doclen_body= QryEval.READER.getSumTotalTermFreq("body") /(float)QryEval.READER.getDocCount("body") ;
    float avg_doclen_title= QryEval.READER.getSumTotalTermFreq("title") /(float)QryEval.READER.getDocCount("title") ;
    float avg_doclen_url= QryEval.READER.getSumTotalTermFreq("url") /(float)QryEval.READER.getDocCount("url") ;
    float avg_doclen_inlink= QryEval.READER.getSumTotalTermFreq("inlink") /(float)QryEval.READER.getDocCount("inlink") ;


    // get feature vectors for training data
    if (modelKind.equals("letor")){
      Scanner trainQry = new Scanner(new File(params.get("letor:trainingQueryFile")));
      BufferedWriter trainWrite = new BufferedWriter(new FileWriter(new File(params.get("letor:trainingFeatureVectorsFile"))));

      do {

        String tQry = trainQry.nextLine();
        String []pairs=tQry.split(":");
        String []queryStems = QryEval.tokenizeQuery(pairs[1]);
        String qid = pairs[0];
       // System.out.println(Arrays.toString(queryStems));
        HashMap <String, String> tmpMap = relFileMap.get(qid);
        Iterator iter = tmpMap.entrySet().iterator();
        Entry<String,String> entry;
        String extDocid = null;
        int inDocid;

        ArrayList <float []> featureVectors= new ArrayList <float[]> ();
        ArrayList<boolean []>checkVectors = new ArrayList<boolean []>();
        ArrayList <String> filenames = new ArrayList<String>();
        ArrayList <String> relevances = new ArrayList<String>();
        HashSet <String>terms= new HashSet<String> ();
        terms.addAll(Arrays.asList(queryStems));

        float []fvMin = new float[18];
        float []fvMax = new float[18];
        Arrays.fill(fvMin,Float.MAX_VALUE);
        Arrays.fill(fvMax, Float.MIN_VALUE);

        while (iter.hasNext()){
          boolean []check= new boolean[18];
          Arrays.fill(check,true);
          float [] fv = new float[18];
          entry=  (Entry<String, String>) iter.next();
          extDocid = entry.getKey();
          filenames.add(extDocid);
          relevances.add(entry.getValue());
          inDocid =  getInternalDocid (extDocid);

          Document d = QryEval.READER.document( inDocid);
          float spamscore = Float.parseFloat(d.get("score"));
          String rawUrl = d.get("rawUrl");

          // f1: spam score
          if (disable[0]){
        	  fv[0]=0;
        	  check[0]=false;
          }else{
        	  fv[0]=spamscore;
          }
          
          // f2: url depth
          if (disable[1]){
        	  fv[1]=0;
        	  check[1]=false;
          }else{
        	  fv[1]= countDepth(rawUrl);
          }
         
          // f3: wikipedia score
          if (disable[2]){
        	  fv[2]=0;
        	  check[2]=false;
          }else{
        	  fv[2]= rawUrl.contains("wikipedia.org") ? 1 : 0;
          }
          
          // f4: pageRank score
          if (disable[3] || !pageRankMap.containsKey(extDocid)){
        	  fv[3]=0;
        	  check[3]=false;
          }else{
        	  fv[3]= pageRankMap.get(extDocid);
          }
          float doclen;
          
          Terms checkTerms = QryEval.READER.getTermVector(inDocid, "body");
          if (checkTerms == null) {
        	  for (int k=4;k<=6;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }
        	  fv[16]=0;
        	  check[16]=false;
          }else{
        	  TermVector tvBody = new TermVector(inDocid,"body");
        	  doclen = QryEval.dls.getDocLength("body", inDocid);
        	  if (disable[4]){
        		  fv[4]=0;
        		  check[4]=false;
        	  }else{
        		// f5: bm25 for <q,d body>
        		  fv[4]= getBM25(terms, model, tvBody, doclen, avg_doclen_body);
        	  }
        	  if (disable[5]){
        		  // f6: indri for <q, d body>
        		  fv[5]=0;
        		  check[5]=false;
                 
        	  }else{
        		  fv[5]= getIndri(terms, model, tvBody, doclen, "body");
        	  }
        	  // f7: term overlap for <q,d Body>
              if (disable[6]){
            	  fv[6]=0;
            	  check[6]=false;
              }else{
            	  fv[6]= getTermOverlap(terms, tvBody, doclen);
            	  
              }
         	  if (disable[16]){
        		  fv[16]=0;
        		  check[16]=false;
        	  }else{
        		  fv[16]=getTFIdf(terms,tvBody);
        	  }
              
          }
          
          checkTerms = QryEval.READER.getTermVector(inDocid, "title");
          if (checkTerms == null) {
        	  for (int k=7;k<=9;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }
        	  
        	  fv[17]=0;
        	  check[17]=false;
          }else{
        	  TermVector tvTitle = new TermVector(inDocid,"title");
        	  doclen = QryEval.dls.getDocLength("title", inDocid);
        	// f8: bm25 for <q,d title>
        	  if (disable[7]){
        		  fv[7]=0;
        		  check[7]=false;
        	  }else{
        		  fv[7]= getBM25(terms,model, tvTitle,doclen,avg_doclen_title);
        	  }
             
              // f9: indri for <q, d title>
        	  if (disable[8]){
        		  fv[8]=0;
        		  check[8]=false;
        	  }else{
        		  fv[8]= getIndri(terms,model,tvTitle,doclen,"title");
        	  }              
              // f10: term overlap for <q,d title>
        	  if (disable[9]){
        		  fv[9]=0;
        		  check[9]=false;
        	  }else{
        		  fv[9]= getTermOverlap(terms,tvTitle,doclen);
        	  }

      	  
        	  if (disable[17]){
        		  fv[17]=0;
        		  check[17]=false;
        	  }else{
        		  fv[17]=getVSMSim(terms,tvTitle);
        	  }
             
          }

          checkTerms = QryEval.READER.getTermVector(inDocid, "url");
          if (checkTerms == null) {
        	  for (int k=10;k<=12;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }
          }else{
        	  TermVector tvUrl = new TermVector(inDocid,"url");            
              doclen = QryEval.dls.getDocLength("url", inDocid);
              
              // f11: bm25 for <q,d url>
              if (disable[10]){
            	  fv[10]=0;
        		  check[10]=false;
              }else{
            	  fv[10]= getBM25(terms,model, tvUrl,doclen,avg_doclen_url);
              }
             
              // f12: Indri for <q, d url>
              if (disable[11]){
            	  fv[11]=0;
        		  check[11]=false;
              }else{
            	  fv[11]= getIndri(terms,model,tvUrl,doclen,"url");
              }
             
              // f13: term overlap for <q,d url>
              if (disable[12]){
            	  fv[12]=0;
        		  check[12]=false;
            	  
              }else{
            	  fv[12]= getTermOverlap(terms,tvUrl,doclen);
              }  
          }

          checkTerms = QryEval.READER.getTermVector(inDocid, "inlink");
          if (checkTerms == null) {
        	  for (int k=13;k<=15;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }

          }else{
        	  TermVector tvInlink = new TermVector(inDocid,"inlink");
              doclen = QryEval.dls.getDocLength("inlink", inDocid);
              // f14: bm25 for <q,d body>
              if (disable[13]){
            	  fv[13]=0;
        		  check[13]=false;
              }else{
            	  fv[13]= getBM25(terms,model, tvInlink,doclen,avg_doclen_inlink);
              }
             
              // f15: indri for <q, d body>
              if (disable[14]){
            	  fv[14]=0;
        		  check[14]=false;
              }else{
            	  fv[14]= getIndri(terms,model,tvInlink,doclen,"inlink");
              }             
              // f16: term overlap for <q,d Body>
              if (disable[15]){
            	  fv[15]=0;
        		  check[15]=false;
              }else{
            	  fv[15]= getTermOverlap(terms,tvInlink,doclen);     
              }
              	  
          }

          featureVectors.add(fv);
          checkVectors.add(check);
          checkMinMax(fv,fvMin,fvMax,check);
          
        }

        normalFV(featureVectors,fvMin,fvMax,disable,checkVectors);
        int size = filenames.size();
        StringBuilder tmp ;
        for (int k=0;k<size;k++){
          tmp = new StringBuilder();
          tmp.append(relevances.get(k)+" qid:"+ qid+ " ");
          float []a= featureVectors.get(k);
          for (int j=0;j<a.length;j++){
            tmp.append((j+1)+":"+a[j]+" ");
          }
          tmp.append("# ").append(filenames.get(k)).append("\n");
          trainWrite.write(tmp.toString());
        }


      }while (trainQry.hasNext());
      trainWrite.close();

      // train call svm rank
      train(params.get("letor:svmRankLearnPath"),params.get("letor:svmRankParamC"), params.get("letor:trainingFeatureVectorsFile"),params.get("letor:svmRankModelFile"));
 
      // generate testing data for top 100 documents in initial BM25 ranking
      Scanner scanQry = new Scanner(new File(params.get("queryFilePath")));
      BufferedWriter testwr = new BufferedWriter(new FileWriter(new File(params.get("letor:testingFeatureVectorsFile"))));
      String qline = null;
      Qryop qTree;
      QryResult result;
   
     // println(m instanceof RetrievalModelBM25);
      // <qid, filenames>
      int resSize=100;
      TreeMap <Integer,String[]> queryFileMap = new TreeMap<Integer,String[]>();
      do {
        qline = scanQry.nextLine();
        String []pairs=qline.split(":");
        String query = pairs[1];
       // System.out.println(query);
       // qTree = parseQueryLeToR (query,m);
        qTree = parseQuery(query,m);
        result = qTree.evaluate(m);
        
        
        // sort
        HashMap <String , Double>resultMap =  new HashMap <String, Double>();
        resSize=result.docScores.scores.size();
        String [] filenames= new String[resSize];
        for (int i =0;i<resSize;i++){
          resultMap.put(getExternalDocid (result.docScores.getDocid(i)), result.docScores.getDocidScore(i));
        }
        
        List<Map.Entry<String, Double>> list= new ArrayList<Map.Entry<String, Double>>(resultMap.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<String, Double>>(){
          public int compare(Map.Entry<String, Double> o1,Map.Entry<String, Double> o2){
            if (o2.getValue() > o1.getValue())
              return 1;
            else if(o2.getValue() < o1.getValue())
              return -1;
            else
              return o1.getKey().compareTo(o2.getKey());

          }
        });
       
        // generate testing data for top 100 documents in initial BM25 ranking
        String []queryStems = QryEval.tokenizeQuery(pairs[1]);
        String qid = pairs[0];
        String extDocid = null;
        int inDocid;

        ArrayList <float []> featureVectors= new ArrayList <float[]> ();
        ArrayList<boolean[]>checkVectors = new ArrayList<boolean[]>();
        HashSet <String>terms= new HashSet<String> ();
        terms.addAll(Arrays.asList(queryStems));

        float []fvMin = new float[18];
        float []fvMax = new float[18];
        Arrays.fill(fvMin,Float.MAX_VALUE);
        Arrays.fill(fvMax, Float.MIN_VALUE);

        for (int i=0;i<100 && i<resSize;i++){
          boolean []check = new boolean[18];
          Arrays.fill(check, true);
          float [] fv = new float[18];
          extDocid = list.get(i).getKey();
          filenames[i]=extDocid;
          inDocid =  getInternalDocid (extDocid);
         
          Document d = QryEval.READER.document( inDocid);
          float spamscore = Float.parseFloat(d.get("score"));
          String rawUrl = d.get("rawUrl");

          // f1: spam score
          if (disable[0]){
        	  fv[0]=0;
        	  check[0]=false;
          }else{
        	  fv[0]=spamscore;
          }
          
          // f2: url depth
          if (disable[1]){
        	  fv[1]=0;
        	  check[1]=false;
          }else{
        	  fv[1]= countDepth(rawUrl);
          }
         
          // f3: wikipedia score
          if (disable[2]){
        	  fv[2]=0;
        	  check[2]=false;
          }else{
        	  fv[2]= rawUrl.contains("wikipedia.org") ? 1 : 0;
          }
          
          // f4: pageRank score
          if (disable[3] || !pageRankMap.containsKey(extDocid)){
        	  fv[3]=0;
        	  check[3]=false;
          }else{
        	  fv[3]= pageRankMap.get(extDocid);
          }
          float doclen;
          
          Terms checkTerms = QryEval.READER.getTermVector(inDocid, "body");
          if (checkTerms == null) {
        	  for (int k=4;k<=6;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }
        	  fv[16]=0;
    		  check[16]=false;
          }else{
        	  TermVector tvBody = new TermVector(inDocid,"body");
        	  doclen = QryEval.dls.getDocLength("body", inDocid);
        	  if (disable[4]){
        		  fv[4]=0;
        		  check[4]=false;
        	  }else{
        		// f5: bm25 for <q,d body>
        		  fv[4]= getBM25(terms, model, tvBody, doclen, avg_doclen_body);
        	  }
        	  if (disable[5]){
        		  // f6: indri for <q, d body>
        		  fv[5]=0;
        		  check[5]=false;
                 
        	  }else{
        		  fv[5]= getIndri(terms, model, tvBody, doclen, "body");
        	  }
        	  // f7: term overlap for <q,d Body>
              if (disable[6]){
            	  fv[6]=0;
            	  check[6]=false;
              }else{
            	  fv[6]= getTermOverlap(terms, tvBody, doclen);
              }
            
        	  if (disable[16]){
        		  fv[16]=0;
        		  check[16]=false;
        	  }else{
        		  fv[16]=getTFIdf(terms,tvBody);
        		
        	  }
             
          }
          
          checkTerms = QryEval.READER.getTermVector(inDocid, "title");
          if (checkTerms == null) {
        	  for (int k=7;k<=9;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }
        	  
        	  fv[17]=0;
        	  check[17]=false;
        	  
          }else{
        	  TermVector tvTitle = new TermVector(inDocid,"title");
        	  doclen = QryEval.dls.getDocLength("title", inDocid);
        	// f8: bm25 for <q,d title>
        	  if (disable[7]){
        		  fv[7]=0;
        		  check[7]=false;
        	  }else{
        		  fv[7]= getBM25(terms,model, tvTitle,doclen,avg_doclen_title);
        	  }
             
              // f9: indri for <q, d title>
        	  if (disable[8]){
        		  fv[8]=0;
        		  check[8]=false;
        	  }else{
        		  fv[8]= getIndri(terms,model,tvTitle,doclen,"title");
        	  }              
              // f10: term overlap for <q,d title>
        	  if (disable[9]){
        		  fv[9]=0;
        		  check[9]=false;
        	  }else{
        		  fv[9]= getTermOverlap(terms,tvTitle,doclen);
        	  }
        	  
        	  if (disable[17]){
        		  fv[17]=0;
        		  check[17]=false;
        	  }else{
        		  fv[17]=getVSMSim(terms,tvTitle);
        	  }
          }

          checkTerms = QryEval.READER.getTermVector(inDocid, "url");
          if (checkTerms == null) {
        	  for (int k=10;k<=12;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }
          }else{
        	  TermVector tvUrl = new TermVector(inDocid,"url");            
              doclen = QryEval.dls.getDocLength("url", inDocid);
              
              // f11: bm25 for <q,d url>
              if (disable[10]){
            	  fv[10]=0;
        		  check[10]=false;
              }else{
            	  fv[10]= getBM25(terms,model, tvUrl,doclen,avg_doclen_url);
              }
             
              // f12: Indri for <q, d url>
              if (disable[11]){
            	  fv[11]=0;
        		  check[11]=false;
              }else{
            	  fv[11]= getIndri(terms,model,tvUrl,doclen,"url");
              }
             
              // f13: term overlap for <q,d url>
              if (disable[12]){
            	  fv[12]=0;
        		  check[12]=false;
            	  
              }else{
            	  fv[12]= getTermOverlap(terms,tvUrl,doclen);
              }  
          }

          checkTerms = QryEval.READER.getTermVector(inDocid, "inlink");
          if (checkTerms == null) {
        	  for (int k=13;k<=15;k++){
        		  fv[k]=0;
        		  check[k]=false;
        	  }

        	  
          }else{
        	  TermVector tvInlink = new TermVector(inDocid,"inlink");
              doclen = QryEval.dls.getDocLength("inlink", inDocid);
              // f14: bm25 for <q,d body>
              if (disable[13]){
            	  fv[13]=0;
        		  check[13]=false;
              }else{
            	  fv[13]= getBM25(terms,model, tvInlink,doclen,avg_doclen_inlink);
              }
             
              // f15: indri for <q, d body>
              if (disable[14]){
            	  fv[14]=0;
        		  check[14]=false;
              }else{
            	  fv[14]= getIndri(terms,model,tvInlink,doclen,"inlink");
              }             
              // f16: term overlap for <q,d Body>
              if (disable[15]){
            	  fv[15]=0;
        		  check[15]=false;
              }else{
            	//  System.out.println(terms+" "+doclen);           	  
            	  fv[15]= getTermOverlap(terms,tvInlink,doclen);     
              }
              	  
          }

          featureVectors.add(fv);
          checkVectors.add(check);
          checkMinMax(fv,fvMin,fvMax,check);
        }
   //     System.out.println(fvMin[16]+" "+fvMax[16]);
        queryFileMap.put(Integer.parseInt(qid),filenames);
        normalFV(featureVectors, fvMin, fvMax,disable,checkVectors);
        
        int size = 100;
        StringBuilder tmp ;
        for (int k=0;k<size && k<resSize;k++){
          tmp = new StringBuilder();
          tmp.append(0+" qid:"+ qid+ " ");
          float []a= featureVectors.get(k);
          for (int j=0;j<a.length;j++){
            tmp.append((j+1)+":"+a[j]+" ");
          }
          tmp.append("# ").append(list.get(k).getKey()).append("\n");
          testwr.write(tmp.toString());
        }

      } while (scanQry.hasNext());
      testwr.close();

      //re-rank test data
      // call svm classify
      classify(params.get("letor:svmRankClassifyPath"), params.get("letor:testingFeatureVectorsFile"),params.get("letor:svmRankModelFile"),params.get("letor:testingDocumentScores"));
      Scanner testScore = new Scanner(new File(params.get("letor:testingDocumentScores")));
      BufferedWriter finalResult = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));
      for (int id: queryFileMap.keySet()){
        String []names=queryFileMap.get(id);
        HashMap <String , Double>resultMap =  new HashMap <String, Double>();
        for (int i=0;i<100 && i<names.length;i++){
          resultMap.put(names[i],Double.parseDouble(testScore.nextLine()));
        }

        List<Map.Entry<String, Double>> list= new ArrayList<Map.Entry<String, Double>>(resultMap.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<String, Double>>(){
          public int compare(Map.Entry<String, Double> o1,Map.Entry<String, Double> o2){
            if (o2.getValue() > o1.getValue())
              return 1;
            else if(o2.getValue() < o1.getValue())
              return -1;
            else
              return o1.getKey().compareTo(o2.getKey());

          }
        });
        String res;
        // the docScores is sorted before.
        for (int i = 0; i < list.size()&& i<100; i++) {
          res=id+"\t" +"Q0\t"
                  + list.get(i).getKey()
                  + "\t"+(i+1)+"\t"
                  + String.format("%.12f",list.get(i).getValue())+"\trun-1";
          System.out.println(res);
          finalResult.write(res + "\n");
        }

      }
      finalResult.close();

    }

    else {
      //  Using the example query parser.  Notice that this does no
      //  lexical processing of query terms.  Add that to the query
      //  parser.
      Scanner scanQry = new Scanner(new File(params.get("queryFilePath")));
      String qline = null;
      Qryop qTree;
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));

      do {
        qline = scanQry.nextLine();
        String []pairs=qline.split(":");
        String oriQuery = pairs[1];
        String query = oriQuery;

        if (fb){

          String expQuery="";
          if (fbRankingFile!=null){
            expQuery = expendQueryWithFile((HashMap<Integer,Double>)fileMap.get(pairs[0]),fbDocs,fbTerms,fbMu);
          }else{
            expQuery = expendQuery(oriQuery,model,fbDocs, fbTerms, fbMu);
          }
          writer1.write(pairs[0]+" : "+expQuery+"\n");
          query=  "#wand ( "+ fbOrigWeight + " #and( "+ oriQuery+ ") "+ (1.0-fbOrigWeight) + " "+  expQuery + " )";
        }

        qTree = parseQuery (query,model);
        printResults (pairs[0], qTree.evaluate (model),writer);

      } while (scanQry.hasNext());
      writer.close();
    }

    
    scan.close();
 
      try {

    	  writer1.close();
      	} catch (Exception e) {
      }

    // Later HW assignments will use more RAM, so you want to be aware
    // of how much memory your program uses.

    printMemoryUsage(false);
    long time2=System.currentTimeMillis();
    long interval=time2-time1;
    // print the running time
    System.out.println(interval);
  }


  /**
   * get f2 url depth for d.
   */
  static float countDepth(String url){
    float count=0;
    char [] set = url.toCharArray();
    for (int i=0;i<set.length;i++){
      if (set[i]=='/')
        count++;
    }
    return count;
  }

  /**
   * getBM25 for Letor
   */
  static float getBM25(HashSet <String>terms,RetrievalModel model, TermVector tv, float doclen, float avg_doclen) throws IOException {

    HashSet <String> t = new HashSet<String>(terms);
    String []stems=tv.stems;
    float N = (float) QryEval.N;
    float score = 0;
    RetrievalModelLeToR m = (RetrievalModelLeToR) model;

    for (int i=1; i< stems.length;i++){
      if (t.contains(stems[i])){
        float df = (float) tv.stemDf(i);
        float tf = (float) tv.stemFreq(i);
         score += Math.max(0, Math.log((N-df+0.5)/(df+0.5))*tf/(tf+ m.k1*((1-m.b)+m.b*doclen/avg_doclen)));
        t.remove(stems[i]);
        if (t.isEmpty()){
          break;
        }
      }

    }
    return score;
  }

  /**
   * getIndri for LeToR
   */
  static float getIndri (HashSet <String>terms,RetrievalModel model, TermVector tv, float doclen, String field) throws IOException {
    int n=terms.size();
    HashSet <String> t = new HashSet<String>(terms);
    String []stems=tv.stems;
    double score = 1;
    RetrievalModelLeToR m = (RetrievalModelLeToR) model;
   
    double C=(double)QryEval.corpus.get(field);

    for (int i=1; i< stems.length;i++){
      if (terms.contains(stems[i])){
        double ctf =(double) tv.totalStemFreq(i);
        double tf = (double) tv.stemFreq(i);
        double Pmle = ctf/C;
        score *= ((1.0-m.lambda)* (tf+m.mu*Pmle)/(doclen+m.mu)+ (m.lambda)*Pmle);
        t.remove(stems[i]);
        if (t.isEmpty()){
          return (float)Math.pow(score, 1.0/n);
        }
      }
    }
    if (t.size()==n)
    	return 0;
    double Pmle;
    for (String tmp : t){
    	Pmle = ((double)QryEval.READER.totalTermFreq (new Term (field, new BytesRef(tmp))))/C;
    	score *=((1.0-m.lambda)* (m.mu*Pmle)/(doclen+m.mu)+ (m.lambda)*Pmle);
    }
    
    return (float)Math.pow(score, 1.0/n);
  }

  /**
   * getTermOverlap for LeToR
   * @param terms
   * @param tv
   * @param doclen
   * @return score
   * @throws IOException
   */
  static float getTermOverlap (HashSet <String>terms, TermVector tv, float doclen) throws IOException {
    float count=0,n=terms.size();
    HashSet <String> t = new HashSet<String>(terms);
    String []stems=tv.stems;
    if (n==0)
      return 0;
    float score = 0;
   
    for (int i=1; i< stems.length;i++){
      if (t.contains(stems[i])){
        count++;
        if (count==n){
          break;
        }
      }
    }
   // System.out.println(count);
    return (float)count/n;
  }
 
  //custom feature, body field
  static float getTFIdf(HashSet<String> terms, TermVector tv) throws IOException{
	  double score=0.0;
	  String []stems=tv.stems;
	  HashSet<String> t = new HashSet<String>(terms);
	  for (int i=1; i< stems.length;i++){
	      if (t.contains(stems[i])){
	        double df =  (double)tv.stemDf(i);
	        double tf =  tv.stemFreq(i);
	         score += tf * Math.log((N+1)/df);
	        t.remove(stems[i]);
	        if (t.isEmpty()){
	          break;
	        }
	      }
	    }
	  
	  return (float) score;
  }
  // custom feature VSM similarity title field
  static float getVSMSim(HashSet<String> terms,TermVector tv) throws IOException{
	  double score=0;
	  double docNom=0,queryNom=0;

	  String []stems=tv.stems;
	  HashSet<String> t = new HashSet<String>(terms);
	  for (int i=1; i< stems.length;i++){
		  double tf =  tv.stemFreq(i);
	      if (t.contains(stems[i])){
	        double df =  (double)tv.stemDf(i);
	        double idf = Math.log(N/df);
	         score += (Math.log(tf)+1.0)*idf;
	         queryNom += Math.pow(idf, 2);
	        t.remove(stems[i]);
	      }

	      docNom += Math.pow(Math.log(tf)+1.0, 2);
	   }
	  if (!t.isEmpty()){
		  for (String tmp: t){
			  double idf = Math.log(N/(double) READER.docFreq(new Term ("title", new BytesRef(tmp))));
			  queryNom += Math.pow(idf , 2);
		  }
	  }
	  
	  return (float) (score/(Math.sqrt(queryNom)*Math.sqrt(docNom)));
	
  }

  /**
   * update the fvMin and fvMax
   * @param fv
   * @param fvMin
   * @param fvMax
   */
  static void checkMinMax(float []fv,float []fvMin, float []fvMax,boolean [] check){
      for (int i=0;i<fv.length;i++){
    	if (check[i]){
    		fvMin[i]=fvMin[i] <= fv[i]? fvMin[i] : fv[i];
            fvMax[i]=fvMax[i] >= fv[i]? fvMax[i] : fv[i];
    	}   
      }
  }

  /**
   * Normalize feature vector
   * @param fvs
   * @param fvMin
   * @param fvMax
   */
  static void normalFV(ArrayList<float[]> fvs, float[]fvMin, float []fvMax,boolean [] disable,ArrayList<boolean []>checkVectors){
    float gap;
    for (int i=0;i<fvMin.length;i++){
    	if (disable[i])
    	continue;
      gap= fvMax[i]-fvMin[i];
      if (gap<=0){
        for (float []a: fvs){
          a[i]=0;
        }
      }else{
        for (int j=0;j< fvs.size();j++){
        float []a=fvs.get(j);
        boolean []c = checkVectors.get(j);
        if (c[i])
          a[i]=(a[i]-fvMin[i])/gap;
        }
      }
    }
  }

  static void train(String svmRankLearnPath,String svmRankParamC,String trainingFeatureVectorsFile,String svmRankModelFile ) throws Exception {
    String line;
    Process cmdProc = Runtime.getRuntime().exec(
            new String[] { svmRankLearnPath, "-c", svmRankParamC , trainingFeatureVectorsFile,
                    svmRankModelFile });

    // consume stdout and print it out for debugging purposes
    BufferedReader stdoutReader = new BufferedReader(
            new InputStreamReader(cmdProc.getInputStream()));

    while ((line = stdoutReader.readLine()) != null) {
      System.out.println(line);
    }
    // consume stderr and print it for debugging purposes
    BufferedReader stderrReader = new BufferedReader(
            new InputStreamReader(cmdProc.getErrorStream()));
    while ((line = stderrReader.readLine()) != null) {
      System.out.println(line);
    }

    // get the return value from the executable. 0 means success, non-zero
    // indicates a problem
    int retValue = cmdProc.waitFor();
    if (retValue != 0) {
      throw new Exception("SVM Rank crashed.");
    }

  }
  
  static void classify(String svmRankClassifyPath,String testingFeatureVectorsFile,String svmRankModelFile,String testingDocumentScores ) throws Exception {
	    String line;
	    Process cmdProc = Runtime.getRuntime().exec(
	            new String[] { svmRankClassifyPath, testingFeatureVectorsFile,svmRankModelFile,
	            		testingDocumentScores });

	    // consume stdout and print it out for debugging purposes
	    BufferedReader stdoutReader = new BufferedReader(
	            new InputStreamReader(cmdProc.getInputStream()));

	    while ((line = stdoutReader.readLine()) != null) {
	      System.out.println(line);
	    }
	    // consume stderr and print it for debugging purposes
	    BufferedReader stderrReader = new BufferedReader(
	            new InputStreamReader(cmdProc.getErrorStream()));
	    while ((line = stderrReader.readLine()) != null) {
	      System.out.println(line);
	    }

	    // get the return value from the executable. 0 means success, non-zero
	    // indicates a problem
	    int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }

	  }

  /**
   *  Write an error message and exit.  This can be done in other
   *  ways, but I wanted something that takes just one statement so
   *  that it is easy to insert checks without cluttering the code.
   *  @param message The error message to write before exiting.
   *  @return void
   */
  static void fatalError (String message) {
    System.err.println (message);
    System.exit(1);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id. If the internal id doesn't exists, returns null.
   *  
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static String getExternalDocid (int iid) throws IOException {
    Document d = QryEval.READER.document (iid);
    String eid = d.get ("externalId");
    return eid;
  }

  /**
   *  Finds the internal document id for a document specified by its
   *  external id, e.g. clueweb09-enwp00-88-09710.  If no such
   *  document exists, it throws an exception. 
   * 
   * @param externalId The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid (String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));
    
    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1,false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    
    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString
   *          A string containing a query.
   * @param qTree
   *          A query tree
   * @throws IOException
   */
  static Qryop parseQuery(String qString, RetrievalModel r) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.
    //1.a If a query has no explicit query operator, default to #OR;

    qString = qString.trim();

    	// add the default or operator directly

    if (r instanceof RetrievalModelBM25)
    	 qString = "#sum(" + qString + ")";
    else if (r instanceof RetrievalModelIndri)
    	qString = "#and(" + qString + ")";
    else
    	qString = "#or(" + qString + ")";


    // Tokenize the query.
    
    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;
    String []tmp;
    
    HashSet <String >fields= new HashSet<String>();
    fields.add("url");
    fields.add("keywords");
    fields.add("title");
    fields.add("inlink");
    fields.add("body");
    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.

    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();

      if (token.matches("[ ,(\t\n\r]")) {
        // Ignore most delimiters.
    	  continue;
      } else if (token.equalsIgnoreCase("#or")) {
          currentOp = new QryopSlOr();
          stack.push(currentOp);
    }	else if (token.equalsIgnoreCase("#and")) {
        currentOp = new QryopSlAnd();
        stack.push(currentOp);
      }
         else if (token.equalsIgnoreCase("#syn")) {
        currentOp = new QryopIlSyn();
        stack.push(currentOp);
      }else if (token.toLowerCase().startsWith("#near/")) {
    	  String []nearn=token.split("/");
         currentOp = new QryopIlNearN(Integer.parseInt(nearn[1]));
          stack.push(currentOp);
        }   else if (token.equalsIgnoreCase("#sum")) {
            currentOp = new QryopSlSum();
            stack.push(currentOp);
          }else if (token.toLowerCase().startsWith("#window/")) {
        	  String []window=token.split("/");
              currentOp = new QryopIlWindow(Integer.parseInt(window[1]));
               stack.push(currentOp);
             }  else if (token.equalsIgnoreCase("#wand")) {
                 currentOp = new QryopSlWAND();
                 stack.push(currentOp);
             } else if (token.equalsIgnoreCase("#wsum")) {
                 currentOp = new QryopSlWSUM();
                 stack.push(currentOp);
             }
      else if (token.startsWith(")")) { // Finish current query operator.
        // If the current query operator is not an argument to
        // another query operator (i.e., the stack is empty when it
        // is removed), we're done (assuming correct syntax - see
        // below). Otherwise, add the current operator as an
        // argument to the higher-level operator, and shift
        // processing back to the higher-level operator.

        stack.pop();

        if (stack.empty())
          break;

        Qryop arg = currentOp;
        currentOp = stack.peek();
        if (arg.args.size()>0)
        	currentOp.add(arg);
        else{
        	 if (currentOp instanceof QryopSlWAND){
				 ( (QryopSlWAND)currentOp).weight.remove(( (QryopSlWAND)currentOp).weight.size()-1);
			 }
			 else if (currentOp instanceof QryopSlWSUM){
				 ( (QryopSlWSUM)currentOp).weight.remove(( (QryopSlWSUM)currentOp).weight.size()-1);
			 }
        }
       
      } else {

        // NOTE: You should do lexical processing of the token before
        // creating the query term, and you should check to see whether
        // the token specifies a particular field (e.g., apple.title).
    	  
    	  if (currentOp instanceof  QryopSlWAND){
        	if (((QryopSlWAND)currentOp).count%2==1){
        		double weight = Double.parseDouble(token);
        		((QryopSlWAND)currentOp).weight.add(weight);
        		 ((QryopSlWAND)currentOp).count++;
        		continue;
        	}
        	  
          }else if (currentOp instanceof  QryopSlWSUM){ 
        	  if (((QryopSlWSUM)currentOp).count%2==1){
          		double weight = Double.parseDouble(token);
          		((QryopSlWSUM)currentOp).weight.add(weight);
          		 ((QryopSlWSUM)currentOp).count++;
          		continue;
          	}
          }
    	  
    	//lexical processing using tokenizeQuery
    	  String field="body";
   
    	  // if the token has specified a field
    	 if (token.contains(".")){
    		 String []termwithField=token.split("\\.");
    		 tmp=tokenizeQuery(termwithField[0]);
    		 if (tmp.length>0)
    	    		currentOp.add(new QryopIlTerm(tmp[0],termwithField[1]));
    		 else {
    			 if (currentOp instanceof QryopSlWAND){
    				 ( (QryopSlWAND)currentOp).weight.remove(( (QryopSlWAND)currentOp).weight.size()-1);
    			 }
    			 else if (currentOp instanceof QryopSlWSUM){
    				 ( (QryopSlWSUM)currentOp).weight.remove(( (QryopSlWSUM)currentOp).weight.size()-1);
    			 }
    		 }
    	 }else{//if the token do not specify a field.
    		 tmp=tokenizeQuery(token);
    	    	if (tmp.length>0)
    	    		currentOp.add(new QryopIlTerm(tmp[0]));
    	    	else{
    	    		 if (currentOp instanceof QryopSlWAND){
        				 ( (QryopSlWAND)currentOp).weight.remove(( (QryopSlWAND)currentOp).weight.size()-1);
        			 }
        			 else if (currentOp instanceof QryopSlWSUM){
        				 ( (QryopSlWSUM)currentOp).weight.remove(( (QryopSlWSUM)currentOp).weight.size()-1);
        			 }
    	    	}
    	 }
      }
      
      if (currentOp instanceof  QryopSlWAND){
    	  ((QryopSlWAND)currentOp).count++;
    	  
      }else if (currentOp instanceof  QryopSlWSUM){ 
    	  ((QryopSlWSUM)currentOp).count++;
      }
      
    }

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.

    if (tokens.hasMoreTokens()) {
      System.err.println("Error:  Query syntax is incorrect.  " + qString);
      return null;
    }

    return currentOp;
  }


  static Qryop parseQueryLeToR(String qString, RetrievalModel r) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    qString = qString.trim();

    // add the default or operator directly
    qString = "#sum(" + qString + ")";
    // Tokenize the query.
    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;
    String []tmp;

    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();
      if (token.matches("[ ,(\t\n\r]")) {
        // Ignore most delimiters.
        continue;
      } else if (token.equalsIgnoreCase("#sum")) {
        currentOp = new QryopSlSum();
        stack.push(currentOp);
      }
      else if (token.startsWith(")")) {
        stack.pop();

        if (stack.empty())
          break;

        Qryop arg = currentOp;
        currentOp = stack.peek();
        currentOp.add(arg);

      } else {
          tmp=tokenizeQuery(token);
          if (tmp.length>0)
            currentOp.add(new QryopIlTerm(tmp[0]));
      }

    }

    if (tokens.hasMoreTokens()) {
      System.err.println("Error:  Query syntax is incorrect.  " + qString);
      return null;
    }

    return currentOp;
  }

  /**
   *  Print a message indicating the amount of memory used.  The
   *  caller can indicate whether garbage collection should be
   *  performed, which slows the program but reduces memory usage.
   *  @param gc If true, run the garbage collector before reporting.
   *  @return void
   */
  public static void printMemoryUsage (boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println ("Memory used:  " +
			((runtime.totalMemory() - runtime.freeMemory()) /
			 (1024L * 1024L)) + " MB");
  }
  
  /**
   * Print the query results. 
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
   * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
   * PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException 
   */
  static void printResults(String queryID, QryResult result,BufferedWriter writer) throws IOException {
	 
	  String res=null;
	  	
	  	// if no result
	    if (result.docScores.scores.size() < 1) {
	    	res=queryID+"\t" +"Q0\tdummy\t"+"1\t0\trun-1";
	    	System.out.println(res);
	    	writer.write(res+"\n");
	    	return;
	    } 
	    
	    // first put the score and external ID into a HashMap
	    // then sort the result
	    HashMap <String , Double>resultMap =  new HashMap <String, Double>();
	    for (int i =0;i<result.docScores.scores.size();i++){
	    	resultMap.put(getExternalDocid (result.docScores.getDocid(i)), result.docScores.getDocidScore(i));
	    	//writer.write(getExternalDocid (result.docScores.getDocid(i))+" "+result.docScores.getDocidScore(i)+"\n");
	    }
	    List<Map.Entry<String, Double>> list= new ArrayList<Map.Entry<String, Double>>(resultMap.entrySet());
	    Collections.sort(list,new Comparator<Map.Entry<String, Double>>(){
	    	public int compare(Map.Entry<String, Double> o1,Map.Entry<String, Double> o2){
	    	    if (o2.getValue() > o1.getValue())  
			          return 1;  
			        else if(o2.getValue() < o1.getValue())  
			          return -1;  
			        else   
			        	return o1.getKey().compareTo(o2.getKey());
			        		
	    	}
	    });
	  
    	// the docScores is sorted before.
      for (int i = 0; i < list.size()&& i<100; i++) {
    	  res=queryID+"\t" +"Q0\t"
   			   + list.get(i).getKey()
   			   + "\t"+(i+1)+"\t"
   			   + String.format("%.12f",list.get(i).getValue())+"\trun-1";
        System.out.println(res);
        writer.write(res+"\n");
      }
    
  }

  /**
   *  Given a query string, returns the terms one at a time with stopwords
   *  removed and the terms stemmed using the Krovetz stemmer. 
   * 
   *  Use this method to process raw query terms. 
   * 
   *  @param query String containing query
   *  @return Array of query tokens
   *  @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
  
  static String expendQuery(String oriQuery,RetrievalModel model, int docs,int terms,double fbmu) throws IOException{
	  String query="";
	  Qryop qTree;
	  qTree = parseQuery (oriQuery,model);
	  QryResult result = qTree.evaluate (model);
	  
	  
		// if no result
	    if (result.docScores.scores.size() < 1) {
	    	return query;
	    } 
	   
	    // first put the score and external ID into a HashMap
	    // then sort the result
	    HashMap <Integer , Double>resultMap =  new HashMap <Integer, Double>();
	    for (int i =0;i<result.docScores.scores.size();i++){
	    	resultMap.put(result.docScores.getDocid(i), result.docScores.getDocidScore(i));
	    }
	    List<Map.Entry<Integer, Double>> list= new ArrayList<Map.Entry<Integer, Double>>(resultMap.entrySet());
	    Collections.sort(list,new Comparator<Map.Entry<Integer, Double>>(){
	    	public int compare(Map.Entry<Integer, Double> o1,Map.Entry<Integer, Double> o2){
	    	    if (o2.getValue() > o1.getValue())  
			          return 1;  
			        else if(o2.getValue() < o1.getValue())  
			          return -1;  
			        else   
			        	return o1.getKey().compareTo(o2.getKey());
			        		
	    	}
	    });
	    ArrayList<Double> storeDoc = new ArrayList<Double>();
	    double extraScore =0;
	    for (int i=0;i<docs;i++){
	    	int docid = list.get(i).getKey();
	    	double tmp= list.get(i).getValue()/((double) dls.getDocLength("body",docid)+fbmu);
	    	storeDoc.add(tmp);
	    	extraScore+=tmp;
	    }
	    
	    double C = (double) corpus.get("body");
	    HashMap <String, Double> tmap = new HashMap<String, Double>();
	    for (int i=0;i<docs;i++){
	    	int docid = list.get(i).getKey();
	    	TermVector tv = new TermVector(docid,"body");
	    	
	    	HashSet<Integer> indexSet = new HashSet<Integer>();
	    	for (int j=1;j< tv.stems.length;j++){
	    		int index = j;
	    		if (index==0 || indexSet.contains(index))
	    			continue;
	    		double score=0.0;
	    		indexSet.add(index);
	    		String term = tv.stemString(index);
	    		if (term.contains(",") || term.contains("."))
	    			continue;
	    		double ctf = (double) tv.totalStemFreq(index);
	    		double tf = (double) tv.stemFreq(index);
	    		double Pmle = ctf/C;
	    		
	    		if (!tmap.containsKey(term)){
	    			score = extraScore*fbmu*Pmle*Math.log(1/Pmle);
	    			tmap.put(term, score);
	    		}else{
	    			score = tmap.get(term);
	    		}
	    		
	    		score += tf*storeDoc.get(i)*Math.log(1/Pmle);
	    		tmap.put(term,score);	
	    	}	
	    }
	    
	    List<Map.Entry<String, Double>> scorelist= new ArrayList<Map.Entry<String, Double>>(tmap.entrySet());
	    Collections.sort(scorelist,new Comparator<Map.Entry<String, Double>>(){
	    	public int compare(Map.Entry<String, Double> o1,Map.Entry<String, Double> o2){
	    	    if (o2.getValue() > o1.getValue())  
			          return 1;  
			        else if(o2.getValue() < o1.getValue())  
			          return -1;  
			        else   
			        	return o1.getKey().compareTo(o2.getKey());
			        		
	    	}
	    });
	    query = "#wand (";
	   for (int i=0;i<terms;i++){
		   query += " " + String.format("%.4f", scorelist.get(i).getValue()) + " " + scorelist.get(i).getKey();
	   }
	   
	  query+= " )";
	  
	  return query;
  }
  
  @SuppressWarnings("unchecked")
public static String expendQueryWithFile(HashMap<Integer,Double> qmap, int docs,int terms,double fbmu) throws IOException{
	  	String query="";
	  	
	  	ArrayList<Double> storeDoc = new ArrayList<Double>();
	    double extraScore =0;
	    
	    Iterator iter = qmap.entrySet().iterator();
	    Map.Entry<Integer, Double> entry;
	    while(iter.hasNext()){
	    	entry= (Map.Entry<Integer, Double>)iter.next();
	    	int docid = entry.getKey();
	    	double tmp= entry.getValue()/((double) dls.getDocLength("body",docid)+fbmu);
	    	storeDoc.add(tmp);
	    	extraScore+=tmp;
	    }
	    
	    double C = (double) corpus.get("body");
	    HashMap <String, Double> tmap = new HashMap<String, Double>();
	    
	    int count=0;
	    iter = qmap.entrySet().iterator();
	    while(iter.hasNext()){
	    	entry= (Map.Entry<Integer, Double>)iter.next();
	    	int docid= entry.getKey();
	    	TermVector tv = new TermVector(docid,"body");
	    	
	    	HashSet<Integer> indexSet = new HashSet<Integer>();
	    	for (int j=1;j< tv.stems.length;j++){
	    		int index = j;
	    		if (index==0 || indexSet.contains(index))
	    			continue;
	    		double score=0.0;
	    		indexSet.add(index);
	    		String term = tv.stemString(index);
	    		if (term.contains(",") || term.contains("."))
	    			continue;
	    		double ctf = (double) tv.totalStemFreq(index);
	    		double tf = (double) tv.stemFreq(index);
	    		double Pmle = ctf/C;
	    		
	    		if (!tmap.containsKey(term)){
	    			score = extraScore*fbmu*Pmle*Math.log(1/Pmle);
	    			tmap.put(term, score);
	    		}else{
	    			score = tmap.get(term);
	    		}
	    		
	    		score += tf*storeDoc.get(count)*Math.log(1/Pmle);
	    		tmap.put(term,score);	
	    	}	
	    	count++;
	    }
	    
	    List<Map.Entry<String, Double>> scorelist= new ArrayList<Map.Entry<String, Double>>(tmap.entrySet());
	    Collections.sort(scorelist,new Comparator<Map.Entry<String, Double>>(){
	    	public int compare(Map.Entry<String, Double> o1,Map.Entry<String, Double> o2){
	    	    if (o2.getValue() > o1.getValue())  
			          return 1;  
			        else if(o2.getValue() < o1.getValue())  
			          return -1;  
			        else   
			        	return o1.getKey().compareTo(o2.getKey());
			        		
	    	}
	    });
	    query = "#wand (";
	   for (int i=0;i<terms;i++){
		   query += " " + String.format("%.4f", scorelist.get(i).getValue()) + " " + scorelist.get(i).getKey();
	   }
	   
	  query+= " )";
	  return query;
  }
 
}