package level1;

import java.util.ArrayList;
import java.util.List;

public class L1Data {

	private List<ArrayList<L1DEntry>> sets;
	private int numberOfSets = 128;
	
	public void initialize() {
		List<ArrayList<L1DEntry>> newSets = new ArrayList<ArrayList<L1DEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<L1DEntry> set = new ArrayList<L1DEntry>(2);
			L1DEntry entry0 = new L1DEntry(0, new byte[32]);
			L1DEntry entry1 = new L1DEntry(0, new byte[32]);
			set.add(0, entry0);
			set.add(1, entry1);
			newSets.add(i, set);
		}
		sets = newSets;
	}
	
	public String getEntryData(int setNum, int entryNum) {
		return this.sets.get(setNum).get(entryNum).toString();
	}

	public List<ArrayList<L1DEntry>> getSets() {
		return sets;
	}

	public void setSets(List<ArrayList<L1DEntry>> sets) {
		this.sets = sets;
	}

	public int getNumberOfSets() {
		return numberOfSets;
	}

	public void setNumberOfSets(int numberOfSets) {
		this.numberOfSets = numberOfSets;
	}
}
