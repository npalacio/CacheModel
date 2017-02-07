package level1;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import general.CacheEntry;
import general.Eviction;
import general.Instruction;
import general.Put;
import general.QItem;
import general.Read;
import general.Write;

public class L1Data {

	private List<ArrayList<CacheEntry>> sets;
	private int numberOfSets = 128;
	
	private Queue<QItem> fromL1C;
	private Queue<QItem> toL1C;
	
	public L1Data(Queue<QItem> fromL1C, Queue<QItem> toL1C) {
		this.fromL1C = fromL1C;
		this.toL1C = toL1C;
		initialize();
	}
	
	public void initialize() {
		List<ArrayList<CacheEntry>> newSets = new ArrayList<ArrayList<CacheEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<CacheEntry> set = new ArrayList<CacheEntry>(2);
			CacheEntry entry0 = new CacheEntry(-1, new byte[32]);
//			CacheEntry entry1 = new CacheEntry(0, new byte[32]);
			CacheEntry entry1 = new CacheEntry(-1, ByteBuffer.allocate(32).putInt(i).array());
			set.add(0, entry0);
			set.add(1, entry1);
			newSets.add(i, set);
		}
		sets = newSets;
	}
	
	public void process() {
		//Poll from the L1C queue and process it
		QItem q = fromL1C.poll();
		if(q != null) {
			Instruction instr = q.getInstruction();
			//Will need to process reads, writes and evictions
			if(instr instanceof Read) {
				processRead((Read) instr);
			} else if(instr instanceof Write) {
				processWrite((Write) instr);
			} else if(instr instanceof Eviction) {
				processEviction((Eviction) instr);
			} else if(instr instanceof Put) {
				processPut((Put) instr);
			}
			
		}
	}
	
	//Grab the data and send it to L1
	private void processRead(Read r) {
		int address = r.getAddress();
		int setNum = getSet(address);
		ArrayList<CacheEntry> set = this.sets.get(setNum);
		//Go to this set, find the matching address and put the data into a QItem
		byte[] data = null;
		for(CacheEntry e : set) {
			if(address == e.getAddress()) {
				data = e.getData().clone();
//				System.out.println("L1D: Returning data for a read, at address " + address + ", data = " + ByteBuffer.wrap(data).getInt());
				QItem q = new QItem(r, data);
				this.toL1C.offer(q);
				//System.out.println("Instruction " + r.getNumber() + ", L1D to L1C: Data from address " + r.getAddress());
				return;
			}
		}
		System.out.println("ERROR: No matching address found in L1Data for read instruction from address: " + address);
	}

	//Change the data at the address to the data in the instruction
	private void processWrite(Write w) {
		int address = w.getAddress();
		int setNum = getSet(address);
		ArrayList<CacheEntry> set = this.sets.get(setNum);
		//Go to this set, find the matching address and put the data into a QItem
		for(CacheEntry e : set) {
			if(address == e.getAddress()) {
				e.setData(w.getData().clone());
				System.out.println("Instruction " + w.getNumber() + ", " + w.toString() + ", L1D: Writing " + ByteBuffer.wrap(w.getData()).getInt() + " to address " + w.getAddress());
				this.toL1C.offer(new QItem(w));
				return;
			}
		}
		System.out.println("ERROR: No matching address found in L1Data for write instruction from address: " + address);
	}
	
	private void processEviction(Eviction e) {
		int address = e.getAddress();
		int setNum = getSet(address);
		ArrayList<CacheEntry> set = this.sets.get(setNum);
		//Go to this set, find the matching address and put the data into a QItem
		byte[] data = null;
		for(CacheEntry entry : set) {
			if(address == entry.getAddress()) {
				if(e.isDirty()){
					//Get the data to pass on
					data = entry.getData().clone();
					e.setData(data);
					QItem q = new QItem(e);
					this.toL1C.offer(q);
					System.out.println("L1D: Evicting (dirty) address " + e.getAddress() + " from L1D, passing back dirty data to L1C");
				} else {
					System.out.println("L1D: Evicting (clean) address " + e.getAddress() + " from L1D, resetting entry");
				}
				//Clear out the data currently there
				entry.setData(new byte[32]);
				entry.setAddress(-1);
				return;
			}
		}
		System.out.println("ERROR: No matching address found in L1Data for evict instruction from address: " + address);
	}
	
	private void processPut(Put p) {
		//L1C will have already sent down an eviction so a spot should already be open for this address, just need to write it in
		int instrAddress = p.getAddress();
		int setNum = getSet(instrAddress);
		ArrayList<CacheEntry> set = this.sets.get(setNum);
		for(CacheEntry entry : set) {
			if(entry.getAddress() == -1) {
				//Open spot
				entry.setAddress(instrAddress);
				entry.setData(p.getData().clone());
				System.out.println("L1D: Putting data from address " + p.getAddress() + " into L1D");
				return;
			}
		}
		//If we reach here we did not find open spot
		System.out.println("ERROR: When processing Put in L1D, open spot not found!");
	}
	
	private int getSet(int addr) {
		int setNum = addr % this.numberOfSets;
		return setNum;
	}
	
	//Getters and Setters
	public String getEntryData(int setNum, int entryNum) {
		return this.sets.get(setNum).get(entryNum).toString();
	}

	public List<ArrayList<CacheEntry>> getSets() {
		return sets;
	}

	public void setSets(List<ArrayList<CacheEntry>> sets) {
		this.sets = sets;
	}

	public int getNumberOfSets() {
		return numberOfSets;
	}

	public void setNumberOfSets(int numberOfSets) {
		this.numberOfSets = numberOfSets;
	}
}
