package project2;

import java.util.LinkedList;
import java.util.Queue;

import general.QItem;

public class NodeQManager {
	
	Queue<BusItem> Resp2L1C = new LinkedList<BusItem>();
	Queue<BusItem> L1C2Resp = new LinkedList<BusItem>();
	Queue<QItem> L1C2Requ = new LinkedList<QItem>();
	Queue<BusItem> IC2L1C = new LinkedList<BusItem>();
	Queue<QItem> L1C2Node = new LinkedList<QItem>();	
	
	public NodeQManager() {
		Initialize();
	}
	
	private void Initialize() {

	}

	//Response --> L1C
	public void Resp2L1CPush(BusItem item) {
		//TODO: Implement
	}
	public BusItem Resp2L1CPull() {
		//TODO: Implement		
	}
	
	//L1C --> Response
	public void L1C2RespPush(BusItem item) {
		//TODO: Implement
	}
	public BusItem L1C2RespPull() {
		//TODO: Implement		
	}

	//L1C --> Request
	public void L1C2RequPush(BusItem item) {
		//TODO: Implement
	}
	public BusItem L1C2RequPull() {
		//TODO: Implement		
	}

	//IC --> L1C
	public void IC2L1CPush(BusItem item) {
		//TODO: Implement
	}
	public BusItem IC2L1CPull() {
		//TODO: Implement		
	}
}
