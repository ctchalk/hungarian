package distributedHungarian;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

public class HungarianTest {

	int n = 100;
	int[][] matrix;

	public void initMatrix() {
		matrix = new int[n][n];
		Random rand = new Random();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int number = rand.nextInt(100) + 1;
				matrix[i][j] = number;
			}
		}
	}
	private void cleanup(Object[] ha) {
		for (int i = 0; i < ha.length - 1; i++) {
			if (ha[i] != null) {
				((Hungarian) ha[i]).Kill();
			}
			((Coordinator) ha[ha.length - 1]).Kill();
		}
	}

	private Object[] init10Hungarians() {
		String host = "127.0.0.1";

		String[] peers = new String[n + 1];
		int[] ports = new int[n + 1];
		Object[] ha = new Object[n + 1];
		for (int i = 0; i < n + 1; i++) {
			ports[i] = 1100 + i;
			peers[i] = host;
		}

		for (int i = 0; i < n; i++) {
			ha[i] = new Hungarian(i, peers, ports, matrix[i], n);
		}
		ha[n] = new Coordinator(n, peers, ports, n);

		return ha;
	}

	@Test
	public void TestBasic() throws IOException {
		BufferedWriter file = new BufferedWriter(new OutputStreamWriter(
	              new FileOutputStream("filename1.txt"), "utf-8"));

		for (int i = 10; i < 100000000; i+= 5) {
			n = i;

			file.write("n = "+ i);
			file.newLine();
			System.out.println(i);

			for (int j = 0; j < 10; j++) {

				initMatrix();

				Sequential seq = new Sequential(matrix, n);

				long startTime_S = System.nanoTime();
				seq.solve();
				long endTime_S = System.nanoTime();
				long duration_S = (endTime_S - startTime_S);
				System.out.print("Run time (sequential): " + duration_S + "\n");
				file.write("Run time (sequential): " + duration_S);
				file.newLine();

				// test distributed

				Object[] ha = init10Hungarians();

				long startTime_D = System.nanoTime();
				((Coordinator) ha[n]).main();
				long endTime_D = System.nanoTime();
				long duration_D = (endTime_D - startTime_D);
				System.out.print("Run time (distributed): " + duration_D + "\n");

				file.write("Run time (distributed): " + duration_D);
				file.newLine();

				cleanup(ha);
			}

			file.newLine();

			file.flush();
		}
		file.close();
	}
}
