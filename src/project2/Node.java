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
		this.RequContr = new RequestController(sb, nodeQman);
		this.RespContr = new ResponseController(sb, nodeQman);
		//START HERE!!!!
		this.L1C = new L1Controller(sb, nodeQman);
	}
	
	//Return true if still stuff to do, false otherwise
	public boolean Process() {
		
	}
	
	public void addInstructions(List<Instruction> instructions) {
		//TODO: Implement method to put these into IC
	}
	
	private void writeToOutputFile() {
		//TODO: Implement
	}

}
