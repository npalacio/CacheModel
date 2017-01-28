package level1;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import general.QItem;

public class L1Controller {

	//Each piece of memory will have an address. That address will tell us what set in L1 it would go in.
	//MemoryBlock will give L1C its address, L1C will see which set it would go in and check there, decide hit or miss 
	//and either go to L1D
	
	//L1C needs to know state of each cache line (valid/invalid, dirty/clean and address of block there)
	//If not the right address and dirty, will need to evict first from L1D then put correct line in that place
	
	//Maybe when L1D and L2D process their queues they always go through all of them to figure out any evictions 
	//before they are replaced?
	
	//On writes to the cache we will need to set dirty bit test
	
	private List<ArrayList<L1CEntry>> sets;
	private int numberOfSets = 128;
	private L1Data backingData;

	private Queue<QItem> toProc;
	private Queue<QItem> toData;
	private Queue<QItem> toL2;

	private Queue<QItem> fromProc;
	private Queue<QItem> fromData;
	private Queue<QItem> fromL2;
	
	public L1Controller(L1Data bd, Queue<QItem> toP, Queue<QItem> toD, Queue<QItem> toL2, 
						Queue<QItem> fromP, Queue<QItem> fromD, Queue<QItem> fromL2) {
		this.backingData = bd;
		
		this.toProc = toP;
		this.toData = toD;
		this.toL2 = toL2;
		
		this.fromProc = fromP;
		this.fromData = fromD;
		this.fromL2 = fromL2;
	}
	
	public void initialize() {
		this.backingData.initialize();
		List<ArrayList<L1CEntry>> newSets = new ArrayList<ArrayList<L1CEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<L1CEntry> set = new ArrayList<L1CEntry>(2);
			L1CEntry entry0 = new L1CEntry(false, false, 0);
			L1CEntry entry1 = new L1CEntry(false, false, 0);
			set.add(0, entry0);
			set.add(1, entry1);
			newSets.add(i, set);
		}
		sets = newSets;
	}
	
	public void printCache() {
		System.out.println("Printing L1 Cache...");
		if(sets == null) {
			System.out.println("\tL1C not initialized!");
			return;
		}
		int i = 0;
		for(List<L1CEntry> set : this.sets) {
			System.out.println("Set " + i);
			int j = 0;
			for(L1CEntry entry : set) {
				System.out.println("\tEntry " + j);
				int L1CAddr = entry.getAddress();
				L1DEntry dataEntry = backingData.getSets().get(i).get(j);
				int L1DAddr = dataEntry.getAddress();
				if(L1CAddr == L1DAddr) {
					System.out.println("\t\tAddress = " + L1CAddr + ", data = " + dataEntry.getData().toString() + 
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
