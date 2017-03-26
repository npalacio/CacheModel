package project2;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import general.Instruction;
import general.QItem;

public class NodeQManager {
	
	Queue<BusItem> Resp2L1C = new LinkedList<BusItem>();
	Queue<BusItem> L1C2Resp = new LinkedList<BusItem>();
	Queue<QItem> L1C2Requ = new LinkedList<QItem>();
	Queue<Instruction> IC2L1C = new LinkedList<Instruction>();
	Queue<QItem> L1C2Node = new LinkedList<QItem>();	
	
	public NodeQManager() {
	}
	
	//Response --> L1C
	public void Resp2L1CPush(BusItem item) {
		if(!Resp2L1C.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q in NodeQManager.Resp2L1CPush");
		}
	}
	public BusItem Resp2L1CPull() {
		BusItem item = this.Resp2L1C.poll();
		return item;
	}
	
	//L1C --> Response
	public void L1C2RespPush(BusItem item) {
		if(!L1C2Resp.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q in NodeQManager.L1C2RespPush");
		}
	}
	public BusItem L1C2RespPull() {
		BusItem item = this.L1C2Resp.poll();
		return item;
	}

	//L1C --> Request
	public void L1C2RequPush(QItem item) {
		if(!L1C2Requ.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q in NodeQManager.L1C2RequPush");
		}
	}
	public QItem L1C2RequPull() {
		QItem item = this.L1C2Requ.poll();
		return item;
	}

	//IC --> L1C
	public void IC2L1CPush(Instruction item) {
		if(!IC2L1C.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q in NodeQManager.IC2L1CPush");
		}
	}
	public Instruction IC2L1CPull() {
		Instruction item = this.IC2L1C.poll();
		return item;
	}
}
