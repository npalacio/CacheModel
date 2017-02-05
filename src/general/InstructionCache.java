package general;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import level1.L1Controller;

public class InstructionCache {

	private List<Instruction> instructions;
	private Queue<QItem> IC2L1C;
	private Queue<QItem> L1C2IC;
	private Queue<QItem> toProc;
	private L1Controller L1C;
	private int loadSize = 4;
	private int currentInstruction;
	private int instrListSize;
	
	public InstructionCache(List<Instruction> instrList, Queue<QItem> toL1C, Queue<QItem> fromL1C, Queue<QItem> toProc, L1Controller L1C) {
		this.instructions = instrList;
		this.IC2L1C = toL1C;
		this.L1C2IC = fromL1C;
		this.toProc = toProc;
		this.L1C = L1C;
		this.currentInstruction = 0;
		this.instrListSize = this.instructions.size();
	}
	
	//Returns true if there are still more instructions to process
	public boolean process() {
		//Load instructions and wait to finish and then check the queue coming back to make sure the instructions make it back
		if(this.currentInstruction < this.instrListSize) {
			//Process next set of instructions
			loadNextInstructions();
			while(this.L1C.areAnyLeft()) {
				this.L1C.process();
			}
			//Grab the processed instructions
			List<QItem> processedInstr = new ArrayList<QItem>();
			System.out.println("Processed " + this.L1C2IC.size() + " instructions...");
			QItem q = this.L1C2IC.poll();
			while(q != null) {
				processedInstr.add(q);
				q = this.L1C2IC.poll();
			}
			for(QItem instr : processedInstr) {
				this.toProc.offer(instr);
			}
			return true;
		}
		return false;
	}

	private void loadNextInstructions() {
		int i = this.currentInstruction;
		int j = 0;
		while(i < this.instrListSize && j < this.loadSize) {
			QItem q = new QItem(instructions.get(i));
			this.IC2L1C.offer(q);
			j++;
			i++;
		}
		System.out.println("Instruction Cache: Loaded " + j + " instructions for L1 to process");
		this.currentInstruction = i;
	}
}
