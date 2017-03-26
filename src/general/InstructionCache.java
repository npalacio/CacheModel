package general;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import level1.L1Controller;
import project2.NodeQManager;

public class InstructionCache {

	private Queue<Instruction> instructions = new LinkedList<Instruction>();
	private NodeQManager nodeQman;
	private StringBuilder sb;
	
	private int loadSize = 4;
	private int currentInstruction;
	
	public InstructionCache(StringBuilder stringB, NodeQManager nodeQmanager) {
		this.sb = stringB;
		this.nodeQman = nodeQmanager;
		this.currentInstruction = 0;
	}
	
	public boolean Process() {
		//TODO: Implement
		//This should take every instruction in our 'cache' and pass it to L1C using NodeQManager
		PassInstructions();
	}
	
	public void AddInstructions(List<Instruction> instructions) {
		//TODO: Implement
		for(Instruction i : instructions) {
			this.instructions.offer(i);
		}
	}
	
//	//Returns true if there are still more instructions to process
//	public boolean process() {
//		//Load instructions and wait to finish and then check the queue coming back to make sure the instructions make it back
//		if(this.currentInstruction < this.instrListSize) {
//			//Process next set of instructions
//			loadNextInstructions();
//			int cycle = 0;
//			while(this.L1C.areAnyLeft()) {
//				System.out.println("********************************************************");
//				System.out.println("CYCLE " + cycle);
//				System.out.println("********************************************************");
//				this.L1C.process();
//				cycle++;
//			}
//			System.out.println("********************************************************");
//			System.out.println("DONE");
//			System.out.println("********************************************************");
//			//Grab the processed instructions
//			List<QItem> processedInstr = new ArrayList<QItem>();
//			System.out.println("Processed " + this.L1C2IC.size() + " instructions...");
//			QItem q = this.L1C2IC.poll();
//			while(q != null) {
//				processedInstr.add(q);
//				q = this.L1C2IC.poll();
//			}
//			for(QItem instr : processedInstr) {
//				this.toProc.offer(instr);
//			}
//			return true;
//		}
//		return false;
//	}
//
//	private void loadNextInstructions() {
//		int i = this.currentInstruction;
//		int j = 0;
//		while(i < this.instrListSize && j < this.loadSize) {
//			QItem q = new QItem(instructions.get(i));
//			this.IC2L1C.offer(q);
//			j++;
//			i++;
//		}
//		System.out.println("Instruction Cache: Loaded " + j + " instructions for L1 to process");
//		this.currentInstruction = i;
//	}
}
