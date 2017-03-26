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
		//TODO: Implement
		boolean ret = false;
		if(ProcessFromBC()) {
			ret = true;
		} if(ProcessFromL1C()) {
			ret = true;
		}
		return ret;
	}

	private boolean ProcessFromBC() {
		//TODO: Implement
		//Grab all items from BC Q:
			//if: data that this node requested = pass to L1C Q
			//if: a request for an ack from another node = pass to L1C Q
		//Be careful with the requests from other nodes coming in, they are shared with all other nodes currently
		//processing that request too
	}
	
	private boolean ProcessFromL1C() {
		//TODO: Implement
		//Grab all items from L1C Q:
			//Should only be acks (requests for data go to requestContr) = pass to BC Q
	}
	
	//TODO: Create methods to communicate with BC that go through node
}
