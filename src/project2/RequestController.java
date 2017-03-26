package project2;

import java.util.Queue;

import general.Instruction;
import general.QItem;

public class RequestController {
	
	private StringBuilder sb;
	private NodeQManager nodeQ;
	private Node parent;
	
	public RequestController(StringBuilder s, NodeQManager nodeQmanager, Node parent) {
		this.sb = s;
		this.nodeQ = nodeQmanager;
		this.parent = parent;
	}
	
	public boolean Process() {
		//TODO: Implement
		boolean ret = false;
		//Grab instructions from L1C and turn them into bus instructions
		if(ProcessFromL1C()) {
			ret = true;
		}
		return ret;
	}
	
	private boolean ProcessFromL1C() {
		boolean ret  = false;
		QItem item = this.nodeQ.L1C2RequPull();
		while(item != null) {
			ret = true;
			ProcessQItem(item);
		}
		return ret;
	}
	
	private void ProcessQItem(QItem item) {
		//This is L1C saying it needs an address from the bus
		Instruction instr = item.getInstruction();
		Integer requAddr = instr.getAddress();
		RequestType type = item.getType();
		if(type == null) {
			System.out.println("ERROR: Inside ProcessQItem in RequController, item grabbed from L1C did not have RequestType set, returning");
			return;
		}
		BusItem br = new BusRequest(requAddr, type);
		this.parent.Requ2BCPush(br);
	}
	
}
