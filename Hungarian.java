package distributedHungarian;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Hungarian implements HungarianRMI, Runnable {

	ReentrantLock mutex;
	String[] peers; // hostname
	int[] ports; // host port
	int me; // index into peers[]

	Registry registry;
	HungarianRMI stub;

	AtomicBoolean dead;// for testing
	AtomicBoolean unreliable;// for testing

	// Your data here
	int[] v, z;
	int row_min;
	int n;

	public Hungarian(int me, String[] peers, int[] ports, int[] v, int n) {

		this.me = me;
		this.peers = peers;
		this.ports = ports;
		this.mutex = new ReentrantLock();
		this.dead = new AtomicBoolean(false);
		this.unreliable = new AtomicBoolean(false);

		// Your initialization code here
		this.v = v;
		this.n = n;
		this.row_min = Integer.MAX_VALUE;
		this.z = new int[n];
		for (int i = 0; i < n; i++)
			z[i] = 0;

		// register peers, do not modify this part
		try {
			System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
			registry = LocateRegistry.createRegistry(this.ports[this.me]);
			stub = (HungarianRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
			registry.rebind("Paxos", stub);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Response Call(String rmi, Request req, int id) {
		Response callReply = null;

		HungarianRMI stub;
		try {
			Registry registry = LocateRegistry.getRegistry(this.ports[id]);
			stub = (HungarianRMI) registry.lookup("Paxos");
			if (rmi.equals("Step1"))
				callReply = stub.Step1(req);
			else if (rmi.equals("Step2"))
				callReply = stub.Step2(req);
			else if (rmi.equals("Step3"))
				callReply = stub.Step3(req);
			else if (rmi.equals("Step31"))
				callReply = stub.Step31(req);
			else if (rmi.equals("Step4"))
				callReply = stub.Step4(req);
			else if (rmi.equals("Step5"))
				callReply = stub.Step5(req);
			else if (rmi.equals("add"))
				callReply = stub.add(req);
			else if (rmi.equals("subtract"))
				callReply = stub.subtract(req);
			else
				System.out.println("Wrong parameters!");
		} catch (Exception e) {
			return null;
		}
		return callReply;
	}

	public void Start(int seq, Object value) {
		Thread agreement = new Thread(this);
		agreement.start();
	}

	@Override
	public void run() {
		mutex.lock();
		try {
		} finally {
			mutex.unlock();
		}

	}

	// RMI handler
	public Response Step1(Request req) {
		for (int j = 0; j < n; j++) {
			if (v[j] < row_min)
				row_min = v[j];
		}
		for (int j = 0; j < n; j++) {
			v[j] -= row_min;
		}
		return new Response(v, me);
	}

	public Response Step2(Request req) {
		int[] min_col = req.v;
		for (int j = 0; j < n; j++) {
			v[j] -= min_col[j];
			if (v[j] == 0)
				z[j] = 1;
		}
		return new Response(z, me);
	}

	public Response Step3(Request req) {
		return new Response(z, me);
	}

	public Response Step31(Request req) {
		return new Response(z, me);
	}

	public Response Step4(Request req) {
		return new Response(z, me);
	}

	public Response Step5(Request req) {
		return new Response(v, me);
	}

	public Response subtract(Request req) {
		int min = req.v[0];
		for (int j = 0; j < n; j++)
			v[j] -= min;
		// re-count 0s
		for (int j = 0; j < n; j++) {
			if (v[j] == 0)
				z[j] = 1;
		}
		return new Response(v, me);
	}

	public Response add(Request req) {
		int min = req.v[0];
		int j = req.v[1];
		v[j] += min;
		// re-count 0s
		for (int i = 0; i < n; i++) {
			if (v[i] == 0)
				z[i] = 1;
		}
		return new Response(v, me);
	}
	
	public void Kill() {
		this.dead.getAndSet(true);
		if (this.registry != null) {
			try {
				UnicastRemoteObject.unexportObject(this.registry, true);
			} catch (Exception e) {
				//System.out.println("None reference");
			}
		}
	}

	public boolean isDead() {
		return this.dead.get();
	}

	public void setUnreliable() {
		this.unreliable.getAndSet(true);
	}

	public boolean isunreliable() {
		return this.unreliable.get();
	}

}
