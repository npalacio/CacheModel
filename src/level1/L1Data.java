package level1;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import general.CacheEntry;

public class L1Data {

	private List<ArrayList<CacheEntry>> sets;
	private int numberOfSets = 128;
	
	public L1Data() {
		initialize();
	}
	
	public void initialize() {
		List<ArrayList<CacheEntry>> newSets = new ArrayList<ArrayList<CacheEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<CacheEntry> set = new ArrayList<CacheEntry>(2);
			CacheEntry entry0 = new CacheEntry(0, new byte[32]);
//			CacheEntry entry1 = new CacheEntry(0, new byte[32]);
			CacheEntry entry1 = new CacheEntry(0, ByteBuffer.allocate(32).putInt(i).array());
			set.add(0, entry0);
			set.add(1, entry1);
			newSets.add(i, set);
		}
		sets = newSets;
	}
	
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
