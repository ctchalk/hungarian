package distributedHungarian;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HungarianRMI extends Remote{
    Response Step1(Request req) throws RemoteException;
    Response Step2(Request req) throws RemoteException;
    Response Step3(Request req) throws RemoteException;
    Response Step31(Request req) throws RemoteException;
    Response Step4(Request req) throws RemoteException;
    Response Step5(Request req) throws RemoteException;
    Response subtract(Request req) throws RemoteException;
    Response add(Request req) throws RemoteException;
}
