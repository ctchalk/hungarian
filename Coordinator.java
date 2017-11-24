package distributedHungarian;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Coordinator implements HungarianRMI {
	ReentrantLock mutex;
	String[] peers; // hostname
	int[] ports; // host port
	int me; // index into peers[]

	int[] min_col;
	int m, min;
	int[][] z_c;
	int[] row, col;
	int[] a;
	int n;

	public Coordinator(int me, String[] peers, int[] ports, int n) {
		this.me = me;
		this.peers = peers;
		this.ports = ports;
		this.mutex = new ReentrantLock();
		this.dead = new AtomicBoolean(false);
		this.unreliable = new AtomicBoolean(false);

		min_col = new int[n];
		z_c = new int[n][2];
		row = new int[n];
		col = new int[n];
		a = new int[n];
		for (int i = 0; i < n; i++) {
			min_col[i] = Integer.MAX_VALUE;
			z_c[i][0] = 0;
			z_c[i][1] = -1;
			row[i] = 0;
			col[i] = 0;
			a[i] = -1;
		}
		m = 0;
		min = Integer.MAX_VALUE;
		this.n = n;
		try {
			System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
			registry = LocateRegistry.createRegistry(this.ports[this.me]);
			stub = (HungarianRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
			registry.rebind("Paxos", stub);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Registry registry;
	HungarianRMI stub;

	AtomicBoolean dead;// for testing
	AtomicBoolean unreliable;// for testing

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

	public void main() {

		// Step 1 - row reduction
		for (int i = 0; i < n; i++) {
			Response res = Call("Step1", new Request(null, me), i);
			// set min_col to minimum from each column
			for (int j = 0; j < n; j++) {
				if (res.v[j] < min_col[j])
					min_col[j] = res.v[j];
			}
		}

		// Step 2 - column reduction
		for (int i = 0; i < n; i++) {
			// send min_col to each process to subtract min_col from that column element
			Call("Step2", new Request(min_col, me), i);
		}
		// loop breaks when assignment is done
		boolean bool = true;
		while (bool) {
			// Step 3
			do {// while z_c is not all zeroes !all_zero(z_c, n));
				for (int i = 0; i < n; i++) {
					z_c[i][0] = 0;
					z_c[i][1] = -1;
				}
				
				for (int i = 0; i < n; i++) {
					Response res = Call("Step3", new Request(null, me), i);
					// Algorithm 6

					// Count number of zeroes in v_i
					int sum = 0;
					for (int j = 0; j < n; j++) {
						sum += res.v[j];
					}

					// If v_i has one zero
					if (sum == 1) {
						for (int j = 0; j < n; j++) {
							// If column j has the zero and the column is unassigned
							if (res.v[j] == 1 && a[j] == -1) {
								// Assign row to column
								a[j] = res.PID;
							}
						}
					}

					// If row was not assigned (or was assigned in previous loop)
					if (!contains(res.PID, a, n)) {
						for (int j = 0; j < n; j++) {
							// If row has zero in column j
							if (res.v[j] == 1 && a[j] == -1) {
								//add 1 to z_c (column sum vector)
								z_c[j][0] += res.v[j];
								
								//keep at least one PID which has a zero in that col
								z_c[j][1] = res.PID;
							}
						}
					}
				}

				// end Algorithm 6
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						if (z_c[j][0] == 1 && !contains(z_c[j][1], a, n) && a[j] == -1) {
							a[j] = z_c[j][1];
						}
					}
				}
				while (has_one(z_c, n)) {
					for (int i = 0; i < n; i++) {
						z_c[i][0] = 0;
						z_c[i][1] = -1;
					}
					// System.out.println("Or here?");
					for (int i = 0; i < n; i++) {
						if (!contains(i, a, n)) {
							// Algorithm 7
							Response res = Call("Step31", null, i);
							for (int j = 0; j < n; j++) {
								if (res.v[j] == 1 && a[j] == -1) {
									z_c[j][0] += res.v[j];
									z_c[j][1] = res.PID;
								}
							}
						}
					}
					for (int j = 0; j < n; j++) {
						if (z_c[j][0] == 1 && !contains(z_c[j][1], a, n) && a[j] == -1) {
							a[j] = z_c[j][1];
						}
					}
				}
				for (int j = 0; j < n; j++) {
					if (z_c[j][0] > 1 && !contains(z_c[j][1], a, n) && a[j] == -1) {
						a[j] = z_c[j][1];
						break;
					}
				}
			} while (!has_zero(z_c, n));

			for (int i = 0; i < n; i++) {
				Response res = Call("Step1", new Request(null, me), i);
			}

			boolean allzeros = true;
			for(int i = 0; i < n; i++) {
					if(z_c[i][0] != n)
						allzeros = false;
			}
			
			if(allzeros) {
				for(int i = 0; i < n; i++) {
					a[i] = i;
				}
			}
			
			if (assignment_done(a, n))
				break;

			for (int i = 0; i < n; i++) {
				if (!contains(i, a, n)) {
					row[i] = 1;
					Response res = Call("Step4", null, i);
					// algorithm 8
					for (int j = 0; j < n; j++) {
						if (res.v[j] == 1) {
							col[j] = 1;
						}
					}
				}
			}
			boolean newmark = false;

			do {
				newmark = false;
				for (int j = 0; j < n; j++) {
					if (col[j] == 1 && a[j] != -1) {
						if (row[a[j]] != 1)
							newmark = true;
						row[a[j]] = 1;
						Response res = Call("Step4", null, a[j]);
						for (int k = 0; k < n; k++) {
							if (res.v[k] == 1 && col[k] != 1) {
								col[k] = 1;
								newmark = true;
							}
						}
						// algorithm 8

					}
				}
			} while (newmark == true); // This part maybe wrong

			for (int j = 0; j < n; j++) {
				if (row[j] == 1)
					row[j] = 0;
				else if (row[j] == 0)
					row[j] = 1;
			}
			min = Integer.MAX_VALUE;

			for (int i = 0; i < n; i++) {
				if (row[i] == 0) {
					Response res = Call("Step5", null, i);
					// Algorithm 9
					for (int j = 0; j < n; j++) {
						if (col[j] == 0 && res.v[j] < min) {
							min = res.v[j];
						}
					}
				}
			}
			for(int i = 0; i < n; i++) {
				if(a[i] == -1 && min == Integer.MAX_VALUE) {
					bool = false;
				}
			}
			
			if (min != Integer.MAX_VALUE) {
				for (int j = 0; j < n; j++) {
					if (row[j] == 0)
						Call("subtract", new Request(new int[] { min }, me), j);
					if (col[j] == 1)
						for (int i = 0; i < n; i++) {
							Response res = Call("add", new Request(new int[] { min, j }, me), i);
						}
				}
			}
			for (int i = 0; i < n; i++) {
				z_c[i][0] = 0;
				z_c[i][1] = -1;
				row[i] = 0;
				col[i] = 0;
				a[i] = -1;
			}
		}

	}

	private boolean has_one(int[][] z_c, int n) {
		for (int j = 0; j < n; j++) {
			if (z_c[j][0] == 1)
				return true;
		}
		return false;
	}

	private boolean assignment_done(int[] a, int n) {
		boolean done = true;
		for (int i = 0; i < n; i++) {
			if (a[i] == -1)
				done = false;
		}
		return done;
	}

	// returns true if array a contains i (length n)
	private boolean contains(int i, int[] a, int n) {
		for (int j = 0; j < n; j++) {
			if (a[j] == i) {
				return true;
			}
		}
		return false;
	}

	private boolean has_zero(int[][] z_c, int n) {
		for (int i = 0; i < n; i++) {
			if (z_c[i][0] == 0)
				return true;
		}
		return false;
	}

	@Override
	public Response Step1(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response Step2(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response Step3(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response Step31(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response Step4(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response Step5(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response subtract(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response add(Request req) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
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
}
