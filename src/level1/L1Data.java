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
		//TODO: Make sure we do not send shallow copy of data
		byte[] data = null;
		for(CacheEntry e : set) {
			if(address == e.getAddress()) {
				data = e.getData();
				QItem q = new QItem(r, data);
				this.toL1C.offer(q);
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
		//TODO: Do not put shallow copy of data in cache
		for(CacheEntry e : set) {
			if(address == e.getAddress()) {
				e.setData(w.getData());
				return;
			}
		}
		System.out.println("ERROR: No matching address found in L1Data for write instruction from address: " + address);
	}
	
	private void processEviction(Eviction e) {
		//TODO: If it is dirty send it to the WB, if it is clean send it to the victim cache
		int address = e.getAddress();
		int setNum = getSet(address);
		ArrayList<CacheEntry> set = this.sets.get(setNum);
		//Go to this set, find the matching address and put the data into a QItem
		byte[] data = null;
		for(CacheEntry entry : set) {
			if(address == entry.getAddress()) {
				//Get the data to pass on
				data = entry.getData().clone();
				//Clear out the data currently there
				entry.setData(ByteBuffer.allocate(32).putInt(0).array());
				entry.setAddress(-1);
				e.setData(data);
				QItem q = new QItem(e, data);
				this.toL1C.offer(q);
				return;
			}
		}
		System.out.println("ERROR: No matching address found in L1Data for evict instruction from address: " + address);
	}
	
	private void processPut(Put p) {
		//TODO: Implement
		//L1C will have already sent down an eviction so a spot should already be open for this address, just need to write it in
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
