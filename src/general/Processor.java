package general;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

import level1.L1Controller;

public class Processor {
	
	private L1Controller L1C;
	
	public static void main(String[] args) {
		String fileName = "input/input001.txt";
		Start(fileName);
	}
	
	private static void Start(String fileName) {
		System.out.println("Reading Instructions...");
		List<Instruction> instructions = ProcessInstructions(fileName);
		System.out.println("Initializing components...");
		Queue<QItem> instrCache2L1C = new LinkedList<QItem>();
		Queue<QItem> L1C2instrCache = new LinkedList<QItem>();
		Queue<QItem> instrCache2proc = new LinkedList<QItem>();
		L1Controller L1C = new L1Controller(L1C2instrCache, instrCache2L1C);
		InstructionCache IC = new InstructionCache(instructions, instrCache2L1C, L1C2instrCache, instrCache2proc, L1C);
		System.out.println("Processing instructions...");
		while(IC.process()) {
			QItem q = instrCache2proc.poll();
			while(q != null) {
				//Print out finished instructions here
				Instruction instr = q.getInstruction();
				printInstruction(q);
				q = instrCache2proc.poll();
			}
		}
		
//		L1C.printL1Cache();
//		L1C.printL2Cache();
//		System.out.println(L1C.getMemoryData(20546));
//		for(Instruction instr : instructions) {
//			proc2L1C.offer(new QItem(instr));
//		}
	}
	
	private static List<Instruction> ProcessInstructions(String fileName) {
		FileInputStream fis = null;
		BufferedReader reader = null;
		List<Instruction> instructions = new ArrayList<Instruction>();
		
		try {
			fis = new FileInputStream(fileName);
			reader = new BufferedReader(new InputStreamReader(fis));
			String line = reader.readLine();
			int i = 0;
			while(line != null) {
//				System.out.println(line);
				String[] tokens = line.split(" ");
				//System.out.println(tokens[0] + " " + tokens[1]);
				Instruction instr = null;
				if(tokens[0].equals("read") && tokens.length == 2) {
					instr = new Read(i, Integer.parseInt(tokens[1]));
				} else if(tokens[0].equals("read") && tokens.length == 3) {
					instr = new Read(i, Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
				} else if(tokens[0].equals("write") && tokens.length == 3) {
					instr = new Write(i, Integer.parseInt(tokens[1]), ByteBuffer.allocate(32).putInt(Integer.parseInt(tokens[2])).array());
				} else {
					System.out.println("Invalid input for instruction");
				}
				instructions.add(instr);
				i++;
				line = reader.readLine();
			}
			System.out.println(instructions.size() + " instructions read in...");
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
	
	private static void printInstruction(QItem q) {
		Instruction instr = q.getInstruction();
		String s = null;
		if(instr instanceof Read) {
			Read r = (Read) instr;
			int offset = r.getByteOffset();
			if(offset == 0) {
				int result = ByteBuffer.wrap(q.getData()).getInt();
				s = "Finished processing instruction " + r.getNumber() + ", read from address " + r.getAddress() + ", with result = " + result;
			} else if(offset > 0) {
				//Byte buffer.get puts the specified byte[] with offset and length into the array that you pass it
				byte[] resultDestination = new byte[32 - offset];
				ByteBuffer.allocate(32).get(resultDestination, offset, 32-offset);
				int result = ByteBuffer.wrap(resultDestination).getInt();
				s = "Finished processing instruction " + r.getNumber() + ", read from address " + r.getAddress() + ", with offset = " + offset + ", with result = " + result;
			} else {
				s = "ERROR: Negative offset given for read";
			}
		} else if(instr instanceof Write) {
			Write w = (Write) instr;
			s = "Finished processing instruction " + w.getNumber() + ", write " + ByteBuffer.wrap(w.getData()).getInt() + " to address " + w.getAddress();
		} else {
			s = "ERROR: L1C returned an instruction other than a Read or Write";
		}
		System.out.println(s);
	}
	
	
}