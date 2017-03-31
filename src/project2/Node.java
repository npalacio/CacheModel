package project2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.List;

import general.Instruction;
import general.InstructionCache;
import general.QItem;
import general.Read;
import general.Write;
import level1.L1Controller;

public class Node {
	
	//String Builder for output
	private StringBuilder sb = new StringBuilder();
	private String outputFile;
	private Integer nodeNum;
	
	//Components of Node
	private RequestController RequContr;
	private ResponseController RespContr;
	private InstructionCache IC;
	private L1Controller L1C;

	//Communication components
	private QManager qman;
	private NodeQManager nodeQman;
	
	public Node(Integer nodeNumber, QManager qmanager) {
		this.nodeNum = nodeNumber;
		this.outputFile = "Node" + nodeNumber + ".txt";
		this.qman = qmanager;
		Initialize();
	}
	
	private void Initialize() {
		this.nodeQman = new NodeQManager();
		this.RequContr = new RequestController(sb, nodeQman, this);
		this.RespContr = new ResponseController(sb, nodeQman, this);
		this.L1C = new L1Controller(sb, nodeQman, this);
		this.IC = new InstructionCache(sb, nodeQman);
	}
	
	//Return true if still stuff to do, false otherwise
	public boolean Process() {
		boolean ret = false;
		if(this.RespContr.Process()) {
			ret = true;
		}
		if(this.IC.Process()){
			ret = true;
		}
		if(this.L1C.Process()) {
			ret = true;
		}
		if(this.RequContr.Process()) {
			ret = true;
		}
		if(this.nodeQman.AreAnyLeft()) {
			ret = true;
		}
		ProcessFinishedInstructions();
		return ret;
	}
	
	private void ProcessFinishedInstructions() {
		QItem item = this.nodeQman.L1C2NodePull();
		while(item != null) {
			ProcessFinishedInstruction(item);
			item = this.nodeQman.L1C2NodePull();
		}
	}
	
	private void ProcessFinishedInstruction(QItem q) {
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
				byte[] resultDestination = new byte[32];
				ByteBuffer b = ByteBuffer.wrap(q.getData().clone());
				b.get(resultDestination, offset, 32-offset);
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
		WriteLine(s);
	}

	public void AddInstructions(List<Instruction> instructions) {
		if(instructions.size() > 0) {
			this.IC.AddInstructions(instructions);
		}
	}
	
	public void Terminate() {
		//TODO: Figure out what all needs to happen at the end here
		WriteToOutputFile();
	}
	
	private void WriteToOutputFile() {
		try{
		    PrintWriter writer = new PrintWriter(this.outputFile, "UTF-8");
		    writer.print(this.sb.toString());
		    writer.close();
		} catch (IOException e) {
			// do something
			System.out.println("ERROR: Error in node " + this.nodeNum + " when trying to write to output file");
		}
	}

	public void Requ2BCPush(BusItem item) {
		this.qman.Requ2BCPush(item, this.nodeNum);
	}

	public void Resp2BCPush(BusItem item) {
		this.qman.Resp2BCPush(item, this.nodeNum);
	}

	public BusItem BC2RespPull() {
		return this.qman.BC2RespPull(this.nodeNum);
	}
	
	public void ProcessL1CWriteBack(BusItem wb) {
		this.qman.WriteBackPush(wb, this.nodeNum);
	}
	
	public BusItem SnoopWriteBacks(Integer address) {
		return this.qman.SnoopWriteBackQueue(this.nodeNum, address);
	}

	public Integer getNodeNum() {
		return nodeNum;
	}

	private void WriteLine(String s) {
		this.sb.append(s + "\n");
	}
	
}
