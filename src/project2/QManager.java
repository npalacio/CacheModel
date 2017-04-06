package project2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class QManager {
	
	private List<List<Queue<BusItem>>> nodeQs;
	private Queue<BusItem> writeBacks;
	private Integer numberOfNodes;

	public QManager(int numberOfNodes) {
		this.numberOfNodes = numberOfNodes;
		Initialize();
	}
	
	public void Initialize() {
		this.nodeQs = new ArrayList<List<Queue<BusItem>>>(this.numberOfNodes);
		CreateQs();
	}
	
	private void CreateQs() {
		this.writeBacks = new LinkedList<BusItem>();
		for(int i = 0; i < this.numberOfNodes; i++) {
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
	
	public boolean AreAnyLeft() {
		if(CheckNodeQs()) {
			return true;
		} else if(!this.writeBacks.isEmpty()) {
			return true;
		}
		return false;
	}
	
	private boolean CheckNodeQs() {
		boolean ret = false;
		for(List<Queue<BusItem>> nodeQs : this.nodeQs) {
			for(Queue<BusItem> q : nodeQs) {
				if(!q.isEmpty()) {
					return true;
				}
			}
		}
		return false;
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
	
	//Nodes --> BC
	public void WriteBackPush(BusItem item, int nodeNum) {
		if(item instanceof BusWriteBack) {
			//put in Q
			this.writeBacks.offer(item);
		} else {
			System.out.print("ERROR: In QManager.WriteBackPush, bus item was not an instance of a BusWriteBack, not putting in Q");
		}
	}
	public BusItem WriteBackPull() {
		BusItem item = this.writeBacks.poll();
		return item;
	}
	
	//This method assumes that there will be at most one write back to any given address
	//Returns a write back if the node and address match what you are looking for, null otherwise
	public BusItem SnoopWriteBackQueue(Integer nodeNum, Integer address) {
		BusWriteBack wb = null;
		List<BusItem> temp = new LinkedList<BusItem>();
		int i = 0;
		for(BusItem item : this.writeBacks) {
			//Store this item in a temp Q that will allow us to put the Q back the way we found it only if it is not what we want
			boolean keep = true;
			if(item instanceof BusWriteBack) {
				//Check nodenum
				BusWriteBack tempWB = (BusWriteBack) item;
				if(wb.getNodeNum() == nodeNum && wb.getAddress() == address) {
					keep = false;
					wb = tempWB;
				}
			}
			if(keep) {
				//Put the item in the temp Q for safe keeping
				temp.add(i, item);
				i++;
			}
		}
		//Put all the items in the temp Q back
		Queue<BusItem> newWBQ = new LinkedList<BusItem>();
		//Need to add them in reverse order to preserve the order of the Q
		for(int j = i - 1; j >= 0; j--) {
			newWBQ.offer(temp.get(j));
		}
		this.writeBacks = newWBQ;
		return wb;
	}
}
