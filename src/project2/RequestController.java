package project2;

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
	
	public byte[] SnoopForWriteBack(Integer address) {
		//Check if there is a write back request in 'your' queue and return the updated data for that
		BusWriteBack wb = (BusWriteBack) this.parent.SnoopWriteBacks(address);
		if(wb != null) {
			byte[] data = wb.getData().clone();
			if(data != null) {
				return data.clone();
			} else {
				System.out.println("ERROR: In RequController.SnoopForWriteBack, write back had no data");
			}
		}
		return null;
	}
	
	private void WriteLine(String s) {
		this.sb.append(s + "\n");
	}
}
