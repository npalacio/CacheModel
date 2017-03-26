package project2;

import java.util.Queue;

import general.Instruction;
import general.QItem;

public class RequestController {
	
	private StringBuilder sb;
	private NodeQManager nodeQ;
	
	public RequestController(StringBuilder s, NodeQManager nodeQmanager) {
		this.sb = s;
		this.nodeQ = nodeQmanager;
	}
	
	public boolean Process() {
		//TODO: Implement
		boolean ret = false;
		//Grab instructions from L1C and turn them into bus instructions
		if(ProcessFromL1C()) {
			ret = true;
		}
		//Push bus instructions to BC Q
		if(ProcessToBus()) {
			ret = true;
		}
		return ret;
	}
	
	//TODO: Create methods to communicate with BC that go through node
	
	private boolean ProcessFromL1C() {
		//TODO: Implement
		boolean ret  = false;
		QItem item = this.nodeQ.L1C2RequPull();
		while(item != null) {
			ret = true;
			ProcessQItem(item);
		}
	}
	
	private void ProcessQItem(QItem item) {
		//This is L1C saying it needs an address from the bus
		Integer requAddr = item.getInstruction().getAddress();
		BusItem br = new BusRequest();
	}
	
	private boolean ProcessToBus() {
		//TODO: Implement
	}
}
