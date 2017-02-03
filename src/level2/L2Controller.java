package level2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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
		
		this.backingData = new L2Data();
		this.mainMemory = new Memory(fromMem, toMem);
		
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
	
	//TODO: Make sure that data being return to L1C is put as the QItem's data, not the instruction since that may or may not have it
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
		//TODO: Tell L2D and memory to process their queues
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
						return;
					} else if(inWb) {
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
						//We sent the data back that L1C requested, we are done
						return;
					}
				} else {
					System.out.println("ERROR: When processing from L1C in L2C, cache address found in L2 but is not valid, stopping process!");
					return;
				}
			} else {
				//Not in L2 at all, pass along request to memory
				this.toMem.offer(q);
				return;
			}
		} else if(instr instanceof Eviction) {
			if(inL2D || inWb) {
				//We have the line, write to it
				if(inL2D) {
					//Create Put instruction and pass it to L2D, we already determined that we have this address in L2D, we are just overwriting it
					Put putInstr = new Put(instrAddress, ((Eviction) instr).getData().clone());
				} else if(inWb) {
					//Just write the values to WB
				}
			} else {
				//We dont have the line but we should (mutual inclusion)
				System.out.println("WARNING: L1 evicted line, but L2 does not have that line (it should with mutual inclusion), continuing process");
				//Evict line that is in its spot in L2D
				ControllerEntry entryToBeOverwritten = this.sets.get(setNum).get(0);
				Eviction eviction = new Eviction(entryToBeOverwritten.getAddress());
				eviction.setDirty(entryToBeOverwritten.isDirty());
				QItem q1 = new QItem(eviction);
				this.toL2D.offer(q1);
				//Put the data there
				entryToBeOverwritten.setAddress(instrAddress);
				entryToBeOverwritten.setDirty(((Eviction) instr).isDirty());
				entryToBeOverwritten.setValid(true);
				entryToBeOverwritten.setLoc(Location.L2D);
				Put putInstr = new Put(instrAddress, ((Eviction) instr).getData().clone());
				QItem q2 = new QItem(putInstr);
				this.toL2D.offer(q2);
				//We made room for this eviction from L1 in L2D and then wrote the value there
				return;
			}
			
		}
	}
	
	private void processFromL2D(QItem q) {
		//TODO: Implement
	}

	private void processFromMem(QItem q) {
		//TODO: Implement
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
