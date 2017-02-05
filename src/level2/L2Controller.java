package level2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import general.QItem;
import general.Read;
import general.Write;
import general.CacheEntry;
import general.ControllerEntry;
import general.Eviction;
import general.Instruction;
import general.Location;
import general.Memory;
import general.Put;

public class L2Controller {
	
	//L2Controller is direct mapped
	//Holds a total of 16KB with 32B blocks = 512 total blocks in L2
	
	private List<ArrayList<ControllerEntry>> sets;
	private int numberOfSets = 512;
	private L2Data backingData;
	private Memory mainMemory;
	
	private int writeBufSize = 8;
	private List<ControllerEntry> writeBuf;
	private List<CacheEntry> writeBufData;

	private Queue<QItem> toL1C;
	private Queue<QItem> fromL1C;
	
	private Queue<QItem> toL2D;
	private Queue<QItem> fromL2D;

	private Queue<QItem> toMem;
	private Queue<QItem> fromMem;
	
	//Create sets of integers to keep track of what data is in L1 and L2
	private Set<Integer> L1Addresses;
	private Set<Integer> L2Addresses;
	
	public L2Controller(Queue<QItem> toL1, Queue<QItem> fromL1) {
		this.toL1C = toL1;
		this.fromL1C = fromL1;
		
		this.toL2D = new LinkedList<QItem>();
		this.fromL2D = new LinkedList<QItem>();
		
		this.toMem = new LinkedList<QItem>();
		this.fromMem = new LinkedList<QItem>();
		
		this.backingData = new L2Data(this.toL2D, this.fromL2D);
		this.mainMemory = new Memory(fromMem, toMem);
		
		this.L1Addresses = new HashSet<Integer>();
		this.L2Addresses = new HashSet<Integer>();
		
		initialize();
	}
	
	private void initialize() {
		//Initialize L2C entries
		List<ArrayList<ControllerEntry>> newSets = new ArrayList<ArrayList<ControllerEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<ControllerEntry> set = new ArrayList<ControllerEntry>(1);
			ControllerEntry entry0 = new ControllerEntry(false, false, Location.L2D, -1);
			set.add(0, entry0);
			newSets.add(i, set);
		}
		sets = newSets;
		this.backingData.initialize();
		//Memory initializes on instantiation
		
		//Initialize write buffer
		List<ControllerEntry> newWB = new ArrayList<ControllerEntry>(this.writeBufSize);
		List<CacheEntry> wbData = new ArrayList<CacheEntry>(this.writeBufSize);
		for(int i = 0; i < this.writeBufSize; i++) {
			ControllerEntry entry = new ControllerEntry(false, false, Location.L2D, -1);
			CacheEntry cEntry = new CacheEntry(-1, new byte[32]);
			newWB.add(i, entry);
			wbData.add(i, cEntry);
		}
		this.writeBuf = newWB;
		this.writeBufData = wbData;
	}
	
	//This is method that processes for L2C, L2D and memory
	public void process() {
		QItem q = this.fromL1C.poll();
		if(q != null) {
			processFromL1C(q);
		}
		q = this.fromL2D.poll();
		if(q != null) {
			processFromL2D(q);
		}
		q = this.fromMem.poll();
		if(q != null) {
			processFromMem(q);
		}
		this.backingData.process();
		this.mainMemory.process();
		maintainMutualInclusion();
	}
	
	private void processFromL1C(QItem q) {
		Instruction instr = q.getInstruction();
		int instrAddress = instr.getAddress();
		//Look for the entry in L1D
		int setNum = getSet(instrAddress);
		ArrayList<ControllerEntry> set = this.sets.get(setNum);
		ControllerEntry matchingEntry = set.get(0);
		boolean inL2D = false;
		boolean inWb = false;
		if(matchingEntry.getAddress() == instrAddress) {
			//Line is present in L1D
			inL2D = true;
		}
		//If not in L1D, look in WB
		if(!inL2D) {
			matchingEntry = null;
			for(ControllerEntry entr : this.writeBuf) {
				if(entr.getAddress() == instrAddress) {
					inWb = true;
					matchingEntry = entr;
				}
			}
		}
		//At this point if the entry was in L2D or WB we have it and we have the booleans to tell us which one
		if(inL2D && inWb) {
			System.out.println("ERROR: When processing from L1C in L2C, cache line was found in both L2C entry and Wb, stopping process");
			return;
		}
		if(instr instanceof Read || instr instanceof Write) {
			//L1C is requesting the data for these instructions, lets see if we have it
			if(inL2D || inWb) {
				if(matchingEntry.isValid()) {
					if(inL2D) {
						//Pass request along to L2D
						this.toL2D.offer(q);
						System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L1C to L2C: HIT in L2D for address " + instr.getAddress());
						return;
					} else if(inWb) {
						System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L1C to L2C: HIT in L2 Write Buffer for address " + instr.getAddress());
						//Grab the data and send it back
						int entryIndex = this.writeBuf.indexOf(matchingEntry);
						CacheEntry dataMatch = this.writeBufData.get(entryIndex);
						if(dataMatch.getAddress() != matchingEntry.getAddress()) {
							System.out.println("ERROR: When processing L1C in L2C the addresses across WB and WBData do not match, stopping process!");
							return;
						}
						byte[] data = dataMatch.getData().clone();
						q.setData(data);
						this.toL1C.offer(q);
						this.L1Addresses.add(instrAddress);
						//We sent the data back that L1C requested, we are done
						return;
					}
				} else {
					System.out.println("ERROR: When processing from L1C in L2C, cache address found in L2 but is not valid, stopping process!");
					return;
				}
			} else {
				//Not in L2 at all, pass along request to memory
				System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L1C to L2C: MISS in L2 for address " + instr.getAddress() + ", fetching from memory");
				this.toMem.offer(q);
				return;
			}
		} else if(instr instanceof Eviction) {
			//L1 got rid of this address, regardless of how we integrate it into L2, it is no longer in L1
			this.L1Addresses.remove(instrAddress);
			if(inL2D || inWb) {
				//We have the line, write to it
				if(inL2D) {
					//Create Put instruction and pass it to L2D, we already determined that we have this address in L2D, we are just overwriting it
					Put putInstr = new Put(instrAddress, ((Eviction) instr).getData().clone());
					QItem q1 = new QItem(putInstr);
					this.toL2D.offer(q1);
					return;
				} else if(inWb) {
					//Just write the values to WB
					int entryIndex = this.writeBuf.indexOf(matchingEntry);
					CacheEntry dataMatch = this.writeBufData.get(entryIndex);
					if(dataMatch.getAddress() != matchingEntry.getAddress()) {
						System.out.println("ERROR: When processing eviction from L1C to L2C, addresses across WB and WBData do not match, stopping process!");
						return;
					}
					//Write the values to data and WB
					matchingEntry.setAddress(instrAddress);
					matchingEntry.setDirty(((Eviction) instr).isDirty());
					matchingEntry.setLoc(Location.WRITE_BUFFER);
					matchingEntry.setValid(true);
					dataMatch.setAddress(instrAddress);
					dataMatch.setData(((Eviction) instr).getData().clone());
					//We cleared the spot and wrote the data we are done
					return;
				}
			} else {
				//We dont have the line but we should (mutual inclusion)
				System.out.println("WARNING: L1 evicted line, but L2 does not have that line (it should with mutual inclusion), continuing process");
				//Evict line that is in its spot in L2D
				ControllerEntry entryToBeOverwritten = this.sets.get(setNum).get(0);
				int entryOldAddress = entryToBeOverwritten.getAddress();
				if(entryOldAddress != -1) {
					Eviction eviction = new Eviction(entryOldAddress);
					eviction.setDirty(entryToBeOverwritten.isDirty());
					QItem q1 = new QItem(eviction);
					this.toL2D.offer(q1);
					if(!entryToBeOverwritten.isDirty()) {
						//L2D will not be sending an eviction back for this address, it will just be clearing out the values
						this.L2Addresses.remove(entryOldAddress);
					}
				}
				//Put the data there
				entryToBeOverwritten.setAddress(instrAddress);
				entryToBeOverwritten.setDirty(((Eviction) instr).isDirty());
				entryToBeOverwritten.setValid(true);
				entryToBeOverwritten.setLoc(Location.L2D);
				Put putInstr = new Put(instrAddress, ((Eviction) instr).getData().clone());
				QItem q2 = new QItem(putInstr);
				this.toL2D.offer(q2);
				this.L2Addresses.add(instrAddress);
				//We made room for this eviction from L1 in L2D and then wrote the value there
				return;
			}
		}
	}
	
	private void processFromL2D(QItem q) {
		Instruction instr = q.getInstruction();
		int instrAddress = instr.getAddress();
		if(instr instanceof Read || instr instanceof Write) {
			//L2D is just returning the data that we need, pass it along to L1C for processing
			if(q.getData() != null) {
				this.toL1C.offer(q);
				this.L1Addresses.add(instrAddress);
				System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L2D to L1C: Data from address " + instr.getAddress());
				return;
			} else {
				System.out.println("ERROR: L1D returned a R/W instruction to L2C without any data on QItem, stopping process!");
				return;
			}
		} else if(instr instanceof Eviction) {
			//We evicted something from L2D and it was dirty so it needs to go to WB
			//Check for open space in WB
			ControllerEntry entryForNewData = null;
			for(ControllerEntry entry : this.writeBuf) {
				if(entry.getAddress() == -1) {
					//This is an open space
					entryForNewData = entry;
				}
			}
			//If no open space, make open space
			CacheEntry cacheEntryForNewData = null;
			if(entryForNewData == null) {
				int indexToEvict = ThreadLocalRandom.current().nextInt(0, this.writeBufSize);
				entryForNewData = this.writeBuf.get(indexToEvict);
				int entryAddress = entryForNewData.getAddress();
				cacheEntryForNewData = this.writeBufData.get(indexToEvict);
				if(cacheEntryForNewData.getAddress() != entryAddress) {
					System.out.println("ERROR: When processing Eviction from L2D to L2C, addresses across WB and WBData do not match, stopping process!");
					return;
				}
				//Create eviction to send to memory
				if(!entryForNewData.isDirty()) {
					System.out.println("WARNING: There is an entry in L2 WB that is not dirty, continuing process");
				}
				Eviction eviction = new Eviction(entryAddress, cacheEntryForNewData.getData().clone());
				eviction.setDirty(true);
				QItem q1 = new QItem(eviction);
				this.toMem.offer(q1);
				this.L2Addresses.remove(entryAddress);
				System.out.println("L2C to Memory: Evicting address " + instrAddress + " from L2 Write Buffer to make room for new entry");
			} else {
				//Get the cache entry corresponding to the controller entry
				int entryIndex = this.writeBuf.indexOf(cacheEntryForNewData);
				cacheEntryForNewData = this.writeBufData.get(entryIndex);
			}
			//At this point we have the controller entry and data entry clear (whether it already was clear or we made it clear)
			//Write eviction data to WB
			boolean isNewDataDirty = ((Eviction) instr).isDirty();
			if(!entryForNewData.isDirty()) {
				System.out.println("WARNING: Eviction coming from L2D to L2C must be dirty but dirty bit on eviction instruction was not set, continuing process");
			}
			byte[] newData = ((Eviction) instr).getData().clone();
			cacheEntryForNewData.setAddress(instrAddress);
			cacheEntryForNewData.setData(newData);
			entryForNewData.setAddress(instrAddress);
			entryForNewData.setDirty(isNewDataDirty);
			entryForNewData.setLoc(Location.L2WB);
			entryForNewData.setValid(true);
			//System.out.println("L2 Write Buffer: ");
		}
	}

	private void processFromMem(QItem q) {
		Instruction instr = q.getInstruction();
		int instrAddress = instr.getAddress();
		int setNum = getSet(instrAddress);
		ControllerEntry entryForNewData = this.sets.get(setNum).get(0);
		int entryAddress = entryForNewData.getAddress();
		if(instr instanceof Read || instr instanceof Write) {
			//Check if spot where this data would go is empty
			if(entryAddress != -1) {
				//If it is not empty, send L2D an eviction for that address, set dirty bit of eviction with dirty bit of address being evicted	
				Eviction eviction = new Eviction(entryAddress);
				QItem q1 = new QItem(eviction);
				this.toL2D.offer(q1);
				//Remove from list of addresses in L2 if it is clean since it will not be going to WB
				if(!entryForNewData.isDirty()) this.L2Addresses.remove(entryAddress);
			}
			//Now that we know we have an open spot, send L2D a Put instruction to store the data that came from memory (QItem.data)
			Put putInstr = new Put(instrAddress, q.getData().clone());
			QItem q2 = new QItem(putInstr);
			this.toL2D.offer(q2);
			this.L2Addresses.add(instrAddress);
			//Also set controller entry data
			entryForNewData.setAddress(instrAddress);
			entryForNewData.setLoc(Location.L2D);
			entryForNewData.setDirty(false);
			entryForNewData.setValid(true);
			//Now that we properly stored the data in L2, we need to pass the instruction along to L1C
			this.toL1C.offer(q);
			this.L1Addresses.add(instrAddress);
			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", Memory to L2C: Data from address " + instrAddress);
		} else {
			System.out.println("ERROR: Memory sent L2C an instruction that was not a read or write, stopping process!");
			return;
		}
	}
	
	private void maintainMutualInclusion() {
		Set<Integer> newL1Set = new HashSet<Integer>(this.L1Addresses);
		Set<Integer> newL2Set = new HashSet<Integer>(this.L2Addresses);
		newL1Set.removeAll(newL2Set);
		for(Integer i : newL1Set) {
			System.out.println("L2C: Evicting address " + i + " from L1 in order to maintain mutual exclusion");
			Eviction e = new Eviction(i);
			e.setFromL2ToL1(true);
			QItem q = new QItem(e);
			this.toL1C.offer(q);
			this.L1Addresses.remove(i);
		}
	}
	
	public boolean areAnyLeft() {
		boolean result = false;
		if(this.toL2D.size() > 0) {
			result = true;
		} else if(this.fromL2D.size() > 0) {
			result = true;
		} else if(this.toMem.size() > 0) {
			result = true;
		} else if(this.fromMem.size() > 0) {
			result = true;
		}
		return result;
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
	
	public int getMemoryData(int addr) {
		return mainMemory.getData(addr);
	}
	
	private int getSet(int address) {
		return this.numberOfSets % 512;
	}
}
