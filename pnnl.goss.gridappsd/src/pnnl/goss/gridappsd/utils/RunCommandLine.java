package pnnl.goss.gridappsd.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

//TODO: Could be moved to GOSS core functionality
public class RunCommandLine {

	static String line = null;
	static String error = null;

	public static void runCommand(String command) throws Exception {
		try {

			Runtime r = Runtime.getRuntime();
			Process p = r.exec(command);

			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader br1 = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			while ((line = br.readLine()) != null) {
				System.out.println(line.trim());
			}

			while ((error = br1.readLine()) != null) {
				System.out.println(error);
			}

		} catch (Exception e) {
			System.out.println("Exception @RunCommandLine:runCommand");
			System.out.println(e.toString());
			e.printStackTrace();
			throw e;
		}
	}

}
