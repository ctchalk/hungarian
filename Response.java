package distributedHungarian;
import java.io.Serializable;
import java.util.Vector;

public class Response implements Serializable {
    static final long serialVersionUID=2L;
    // your data here
    int[] v;
    int PID;

    // Your constructor and methods here
    public Response(int[] v, int PID){
    	this.v = v;
        this.PID = PID;
    }
}
