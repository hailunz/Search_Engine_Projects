/**
 * Created by hailunzhu on 4/7/15.
 */
public class RetrievalModelLeToR extends RetrievalModel {

    // parameters for BM25
    public double k1;
    public double k3;
    public double b;

    // parameters for Indri
    public double mu;
    public double lambda;

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
        return false;
    }

    @Override
    public boolean setParameter(String parameterName, String value) {
        return false;
    }
}
