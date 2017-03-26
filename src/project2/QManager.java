package project2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class QManager {
	
	private List<List<Queue<BusItem>>> nodeQs;
	private Integer nodeNum;

	public QManager(int numberOfNodes) {
		this.nodeNum = numberOfNodes;
		Initialize();
	}
	
	public void Initialize() {
		this.nodeQs = new ArrayList<List<Queue<BusItem>>>(this.nodeNum);
		CreateQs();
	}
	
	private void CreateQs() {
		for(int i = 0; i < this.nodeNum; i++) {
			CreateNodeQs(i);
		}
		
	}
	
	//Q0 = Request2BC
	//Q1 = BC2Response
	//Q2 = Response2BC
	private void CreateNodeQs(int i) {
		Queue<BusItem> Request2BC = new LinkedList<BusItem>();
		Queue<BusItem> BC2Response = new LinkedList<BusItem>();
		Queue<BusItem> Response2BC = new LinkedList<BusItem>();
		List<Queue<BusItem>> Qs = new ArrayList<Queue<BusItem>>();
		Qs.add(0, Request2BC);
		Qs.add(1, BC2Response);
		Qs.add(2, Response2BC);
		this.nodeQs.add(i, Qs);
	}
	
	//BC --> ResponseController
	public void BC2RespPush(BusItem item, int nodeNum) {
		List<Queue<BusItem>> Qs = this.nodeQs.get(nodeNum);
		Queue<BusItem> BC2Resp = Qs.get(1);
		if(!BC2Resp.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q for node " + nodeNum + " in QManager.BC2RespPush");
		}
	}
	public BusItem BC2RespPull(int nodeNum) {
		List<Queue<BusItem>> Qs = this.nodeQs.get(nodeNum);
		Queue<BusItem> BC2Resp = Qs.get(1);
		BusItem bi = BC2Resp.poll();
		return bi;
	}

	//ResponseController --> BC
	public void Resp2BCPush(BusItem item, int nodeNum) {
		List<Queue<BusItem>> Qs = this.nodeQs.get(nodeNum);
		Queue<BusItem> Resp2BC = Qs.get(2);
		if(!Resp2BC.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q for node " + nodeNum + " in QManager.Resp2BCPush");
		}
	}
	public BusItem Resp2BCPull(int nodeNum) {
		List<Queue<BusItem>> Qs = this.nodeQs.get(nodeNum);
		Queue<BusItem> Resp2BC = Qs.get(2);
		BusItem bi = Resp2BC.poll();
		return bi;
	}
	
	//RequestController --> BC
	public void Requ2BCPush(BusItem item, int nodeNum) {
		List<Queue<BusItem>> Qs = this.nodeQs.get(nodeNum);
		Queue<BusItem> Requ2BC = Qs.get(0);
		if(!Requ2BC.offer(item)) {
			System.out.println("ERROR: Failed to add item to Q for node " + nodeNum + " in QManager.Requ2BCPush");
		}
	}
	public BusItem Requ2BCPull(int nodeNum) {
		List<Queue<BusItem>> Qs = this.nodeQs.get(nodeNum);
		Queue<BusItem> Requ2BC = Qs.get(0);
		BusItem bi = Requ2BC.poll();
		return bi;
	}
	
}
