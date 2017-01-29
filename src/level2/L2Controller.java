package level2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import general.QItem;
import general.CacheEntry;
import general.ControllerEntry;

public class L2Controller {
	
	//L2Controller is direct mapped
	//Holds a total of 16KB with 32B blocks = 512 total blocks in L2
	
	private List<ArrayList<ControllerEntry>> sets;
	private int numberOfSets = 512;
	private L2Data backingData;

	private Queue<QItem> toL1C;
	private Queue<QItem> fromL1C;
	
	private Queue<QItem> toL2D;
	private Queue<QItem> fromL2D;

	private Queue<QItem> toMem;
	private Queue<QItem> fromMem;
	
	public L2Controller(Queue<QItem> toL1, Queue<QItem> fromL1) {
		this.toL1C = toL1;
		this.fromL1C = fromL1;
		
		this.toL2D = new LinkedList<QItem>();
		this.fromL2D = new LinkedList<QItem>();
		
		this.toMem = new LinkedList<QItem>();
		this.fromMem = new LinkedList<QItem>();
		
		this.backingData = new L2Data();
		initialize();
	}
	
	private void initialize() {
		List<ArrayList<ControllerEntry>> newSets = new ArrayList<ArrayList<ControllerEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<ControllerEntry> set = new ArrayList<ControllerEntry>(1);
			ControllerEntry entry0 = new ControllerEntry(false, false, 0);
			set.add(0, entry0);
			newSets.add(i, set);
		}
		sets = newSets;
	}
	
	public void printCache() {
		System.out.println("Printing L2 Cache...");
		if(sets == null) {
			System.out.println("\tL1C not initialized!");
			return;
		}
		int i = 0;
		for(List<ControllerEntry> set : this.sets) {
			System.out.println("Set " + i);
			int j = 0;
			for(ControllerEntry entry : set) {
				System.out.println("\tEntry " + j);
				int L1CAddr = entry.getAddress();
				CacheEntry dataEntry = backingData.getSets().get(i).get(j);
				int L1DAddr = dataEntry.getAddress();
				if(L1CAddr == L1DAddr) {
					System.out.println("\t\tAddress = " + L1CAddr + ", data = " + java.nio.ByteBuffer.wrap(dataEntry.getData()).getInt() + 
									   ", isValid = " + entry.isValid() + ", isDirty = " + entry.isDirty());
				} else {
					System.out.println("ERROR: Controller address = " + L1CAddr + ", Data address = " + L1DAddr);
				}
				j++;
			}
			i++;
		}
	}
}
