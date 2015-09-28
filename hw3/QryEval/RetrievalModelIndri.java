
public class RetrievalModelIndri extends RetrievalModel {
	
	public double mu;
	public double lambda;
	@Override
	public boolean setParameter(String parameterName, double value) {
		// TODO Auto-generated method stub
		if (parameterName.equals("mu")){
			if (value>=0){
				this.mu=value;
				return true;
			}
			else
				return false;
		}
		else if (parameterName.equals("lambda")){
			if (value>=0 && value <=1.0){
				this.lambda=value;
				return true;
			}
			else
				return false;
		}
		System.err.println ("Error: Unknown parameter name for retrieval model " +
				"Indri: " +
				parameterName);
		return false;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

}
