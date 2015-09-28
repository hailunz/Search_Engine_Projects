
public class RetrievalModelBM25 extends RetrievalModel {

	public double k1;
	public double k3;
	public double b;
	
	@Override
	public boolean setParameter(String parameterName, double value) {
		
		if (parameterName.equals("k1")){
			if (value>=0){
				this.k1=value;
				return true;
			}
			else
				return false;
		}
		else if (parameterName.equals("k3")){
			if (value>=0){
				this.k3=value;
				return true;
			}
			else
				return false;
		}
		else if (parameterName.equals("b")){
			if (value>=0 && value <=1.0){
				this.b=value;
				return true;
			}
			else
				return false;
		}
		System.err.println ("Error: Unknown parameter name for retrieval model " +
				"MB25: " +
				parameterName);
		return false;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

}
