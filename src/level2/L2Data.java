package level2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import general.CacheEntry;

public class L2Data {
	private List<ArrayList<CacheEntry>> sets;
	private int numberOfSets = 512;
	
	public L2Data() {
		initialize();
	}
	
	private void initialize() {
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
