package distributedHungarian;
import java.io.Serializable;

public class Request implements Serializable {
    static final long serialVersionUID=1L;
    int[] v;
    int PID;

    // Your constructor and methods here
    public Request(int[] v, int PID){
    	this.v = v;
    	this.PID = PID;
    }

    // Your constructor and methods here
}
