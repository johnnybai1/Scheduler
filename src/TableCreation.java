import java.io.*;
import java.util.*;

class TableCreation {

	public static void main(String[] args) throws IOException {
		int number_of_processes = Integer.parseInt(args[0]);
		String table_type = args[1];
		String file_name = args[0]+"_"+args[1]+".txt"; 
		PrintWriter writer = new PrintWriter(file_name, "UTF-8");
		writer.println(number_of_processes);
		Random random = new Random();
		
		int process_arrival = 0;

		for (int i = 0; i< number_of_processes; i++){
			int[] burst = new int[5];
			// random total burst from 100 to 1100
			//int total_burst = 100 + random.nextInt(1000);
			// in next 0-10 seconds there is a new process
			process_arrival += random.nextInt(10);
			//randome process id
			int process_priority = random.nextInt(3);
			
			
			//random burst time, at least 1 burst 1 block
			burst[0] = 1 + random.nextInt(100);
			burst[1] = 1 + random.nextInt(burst[0]);
			
			burst[2] = random.nextInt(100);
			//if there is no 2nd burst, no 2nd block
			burst[3] = (burst[2] == 0) ?  0 : random.nextInt(burst[2]);
			//if there is no 2nd block, no 3rd burst
			burst[4] = (burst[3] == 0) ?  0 : 1+ random.nextInt(50);

			writer.println(i +"\t"+process_arrival+"\t"+process_priority+"\t"+burst[0]+"\t"+burst[1]+"\t"+burst[2]+"\t"+burst[3]+"\t"+burst[4]);

		}

		writer.close();
		System.out.println(number_of_processes + table_type);
	}


}