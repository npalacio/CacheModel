package level2;

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

public class L2Data {
	private List<ArrayList<CacheEntry>> sets;
	private int numberOfSets = 512;
	
	private Queue<QItem> fromL2C;
	private Queue<QItem> toL2C;
	
	public L2Data(Queue<QItem> fL2C, Queue<QItem> tL2C) {
		this.fromL2C = fL2C;
		this.toL2C = tL2C;
		initialize();
	}
	
	public void initialize() {
		List<ArrayList<CacheEntry>> newSets = new ArrayList<ArrayList<CacheEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<CacheEntry> set = new ArrayList<CacheEntry>(2);
//			CacheEntry entry0 = new CacheEntry(-1, new byte[32]);
			CacheEntry entry0 = new CacheEntry(-1, ByteBuffer.allocate(32).putInt(i).array());
			set.add(0, entry0);
			newSets.add(i, set);
		}
		sets = newSets;
	}
	
	public void process() {
		QItem q = this.fromL2C.poll();
		if(q != null) {
			processFromL2C(q);
		}
	}
	
	private void processFromL2C(QItem q) {
		Instruction instr = q.getInstruction();
		int instrAddress = instr.getAddress();
		int setNum = getSet(instrAddress);
		CacheEntry entry = this.sets.get(setNum).get(0);
		int entryAddress = entry.getAddress();
		boolean addressMatch = instrAddress == entryAddress;
		if(instr instanceof Read || instr instanceof Write) {
			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L2D: Fetching data from address " + instrAddress);
			if(!addressMatch) {
				System.out.println("ERROR: When retreiving data for R/W in L2D, the instruction address did not match the address in the L2Entry, stopping process!");
				return;
			}
			byte[] data = entry.getData().clone();
			q.setData(data);
			this.toL2C.offer(q);
			//We retrieved the data and passed it along, we are done
			return;
		} else if(instr instanceof Put) {
			if(entryAddress == -1 || addressMatch) {
				//The spot is either open or we are overwriting the same address
				entry.setAddress(instrAddress);
				entry.setData(((Put) instr).getData().clone());
				System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L2D: Putting data for address " + instrAddress + " into L2D");
				//We stored the data we are done
				return;
			} else {
				System.out.println("ERROR: When processing Put instruction in L2D, the cache entry was not open and did not match the address already in entry");
			}
		} else if(instr instanceof Eviction) {
			//Make sure instruction address matches the entry address
			if(!addressMatch) {
				System.out.println("ERROR: When processing eviction in L2D, the instruction address did not match the address in the L2Entry, stopping process!");
				return;				
			}
			if(((Eviction) instr).isDirty()) {
				//We need to put data on eviction instruction and send it back to L2C
				byte[] dataToSave = entry.getData().clone();
				((Eviction) instr).setData(dataToSave);
				this.toL2C.offer(q);
			}
			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L2D: Evicting address " + instrAddress);
			//If it was dirty we saved the data and passed it back
			//Now we just need to clear the values
			entry.setAddress(-1);
			entry.setData(new byte[32]);
		}
	}
	
	private int getSet(int address) {
		return this.numberOfSets % 512;
	}
	
	/* Getters and Setters */
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
