package general;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Processor {
	
	public static void main(String[] args) {
		String fileName = "input/input001.txt";
		Start(fileName);
	}
	
	private static void Start(String fileName) {
		List<Instruction> instructions = ProcessInstructions(fileName);
	}
	
	private static List<Instruction> ProcessInstructions(String fileName) {
		FileInputStream fis = null;
		BufferedReader reader = null;
		List<Instruction> instructions = new ArrayList<Instruction>();
		
		try {
			fis = new FileInputStream(fileName);
			reader = new BufferedReader(new InputStreamReader(fis));
			String line = reader.readLine();
			while(line != null) {
//				System.out.println(line);
				String[] tokens = line.split(" ");
				//System.out.println(tokens[0] + " " + tokens[1]);
				Instruction instr = null;
				if(tokens[0].equals("read") && tokens.length == 2) {
					instr = new Read(Integer.parseInt(tokens[1]));
				} else if(tokens[0].equals("write") && tokens.length == 3) {
					instr = new Write(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
				} else {
					System.out.println("Invalid input for instruction");
				}
				instructions.add(instr);
				//System.out.println("Instruction: isRead=" + instr.isRead() + ", address=" + instr.getAddress());
				line = reader.readLine();
			}
//			System.out.println(instructions.size() + " instructions given");
//			for(Instruction i : instructions) {
//				System.out.println(i.toString());
//			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				reader.close();
			} catch(IOException e) {
				System.out.println(e);
			}
		}
		return instructions;
	}
	
	
}