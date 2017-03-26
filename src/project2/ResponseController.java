package project2;

import java.util.Queue;

public class ResponseController {
	
	private StringBuilder sb;
	private NodeQManager qman;
	private Node parent;
	
	public ResponseController(StringBuilder s, NodeQManager nodeQmanager, Node parent) {
		this.sb = s;
		this.qman = nodeQmanager;
		this.parent = parent;
	}
	
	public boolean Process() {
		boolean ret = false;
		if(ProcessFromBC()) {
			ret = true;
		} if(ProcessFromL1C()) {
			ret = true;
		}
		return ret;
	}

	private boolean ProcessFromBC() {
		//Grab all items from BC Q
		//Be careful with the requests from other nodes coming in, they are shared with all other nodes currently
		//processing that request too
		boolean ret = false;
		BusItem item = this.parent.BC2RespPull();
		while(item != null) {
			ret = true;
			ProcessBCItem(item);
		}
		return ret;
	}
	
	private boolean ProcessFromL1C() {
		//Grab all items from L1C Q:
			//Should only be acks (requests for data go to requestContr) = pass to BC Q
		boolean ret = false;
		BusItem item = this.qman.L1C2RespPull();
		while(item != null) {
			ret = true;
			ProcessL1CItem(item);
		}
		return ret;
	}
	
	private void ProcessBCItem(BusItem item) {
		if(item instanceof BusAcks || item instanceof BusRequest) {
			this.qman.Resp2L1CPush(item);
		} else {
			System.out.println("ERROR: In response controller, BC sent item that was not BusAcks or BusRequest, continuing processing");
		}
	}
	
	private void ProcessL1CItem(BusItem item) {
		if(item instanceof BusAck) {
			this.parent.Resp2BCPush(item);
		} else {
			System.out.println("ERROR: In response controller, L1C sent item that was not BusAck, continuing processing");
		}
	}
}
