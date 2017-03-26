package project2;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import general.Instruction;
import general.InstructionCache;
import general.QItem;
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
		this.L1C = new L1Controller(sb, nodeQman);
		this.IC = new InstructionCache(sb, nodeQman);
	}
	
	//Return true if still stuff to do, false otherwise
	public boolean Process() {
		//TODO: Implement
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
		return ret;
	}
		
	public void AddInstructions(List<Instruction> instructions) {
		//TODO: Implement method to put these into IC
		if(instructions.size() > 0) {
			this.IC.AddInstructions(instructions);
		}
	}
	
	private void WriteToOutputFile() {
		//TODO: Implement
	}

	//TODO: Create methods that Requ/Resp controllers can call to communicate with BC since only the node
	//knows what number it is
	public void Requ2BCPush(BusItem item) {
		this.qman.Requ2BCPush(item, this.nodeNum);
	}

	public void Resp2BCPush(BusItem item) {
		this.qman.Resp2BCPush(item, this.nodeNum);
	}

	public BusItem BC2RespPull() {
		return this.qman.BC2RespPull(this.nodeNum);
	}

	public Integer getNodeNum() {
		return nodeNum;
	}
	
}
