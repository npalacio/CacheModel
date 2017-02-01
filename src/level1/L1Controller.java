package level1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import general.CacheEntry;
import general.ControllerEntry;
import general.Eviction;
import general.Instruction;
import general.Location;
import general.QItem;
import general.Read;
import general.Write;
import level2.L2Controller;

public class L1Controller {

	//Each piece of memory will have an address. That address will tell us what set in L1 it would go in.
	//MemoryBlock will give L1C its address, L1C will see which set it would go in and check there, decide hit or miss 
	//and either go to L1D
	
	//L1C needs to know state of each cache line (valid/invalid, dirty/clean and address of block there)
	//If not the right address and dirty, will need to evict first from L1D then put correct line in that place
	
	//Maybe when L1D and L2D process their queues they always go through all of them to figure out any evictions 
	//before they are replaced?
	
	//On writes to the cache we will need to set dirty bit test
	
	private List<ArrayList<ControllerEntry>> sets;
	private int numberOfSets = 128;
	private L1Data backingData;

	private List<ControllerEntry> writeBuf;
	private List<CacheEntry> writeBufData;
	
	private List<ControllerEntry> victim;
	private List<CacheEntry> victimData;

	private int bufVicSize = 4;

	private Queue<QItem> toProc;
	private Queue<QItem> fromProc;

	private Queue<QItem> toData;
	private Queue<QItem> toL2;

	private Queue<QItem> fromData;
	private Queue<QItem> fromL2;
	
	private L2Controller L2C;
	
	//Create a mapping of addresses to queues so that instructions going to the same address
	//that is not in the cache can all wait in line together for it
	private Map<Integer, Queue<Instruction>> instructionMisses;
	
	//Since processor owns L1C it will pass in the queues to communicate with it
	//L1C initializes the other queues
	public L1Controller(Queue<QItem> toP, Queue<QItem> fromP) {		
		this.toProc = toP;
		this.fromProc = fromP;

		this.toData = new LinkedList<QItem>();
		this.fromData = new LinkedList<QItem>();

		this.toL2 = new LinkedList<QItem>();
		this.fromL2 = new LinkedList<QItem>();
				
		this.backingData = new L1Data(this.toData, this.fromData);
		this.L2C = new L2Controller(this.fromL2, this.toL2);
		initialize();
	}
	
	private void initialize() {
		//Initialize L1Controller entries
		List<ArrayList<ControllerEntry>> newSets = new ArrayList<ArrayList<ControllerEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<ControllerEntry> set = new ArrayList<ControllerEntry>(2);
			ControllerEntry entry0 = new ControllerEntry(false, false, Location.L1D, -1);
			ControllerEntry entry1 = new ControllerEntry(false, false, Location.L1D, -1);
			set.add(0, entry0);
			set.add(1, entry1);
			newSets.add(i, set);
		}
		sets = newSets;
		
		//Initialize writeBuf and victim caches
		List<ControllerEntry> wBuf = new ArrayList<ControllerEntry>(this.bufVicSize);
		List<ControllerEntry> vic = new ArrayList<ControllerEntry>(this.bufVicSize);
		List<CacheEntry> wBData = new ArrayList<CacheEntry>(this.bufVicSize);
		List<CacheEntry> vicData = new ArrayList<CacheEntry>(this.bufVicSize);
		for(int i = 0; i < this.bufVicSize; i++) {
			ControllerEntry wBufEntry = new ControllerEntry(false, false, Location.WRITE_BUFFER, -1);
			ControllerEntry vicEntry = new ControllerEntry(false, false, Location.VICTIM, -1);
			CacheEntry wBufE = new CacheEntry(-1, new byte[32]);
			CacheEntry vicE = new CacheEntry(-1, new byte[32]);
			wBuf.add(i, wBufEntry);
			vic.add(i, vicEntry);
			wBData.add(i, wBufE);
			vicData.add(i, vicE);
		}
		this.writeBuf = wBuf;
		this.victim = vic;
		this.writeBufData = wBData;
		this.victimData = vicData;
		
		//Initialize waiting line for instructions that miss going to the same address
		this.instructionMisses = new HashMap<Integer, Queue<Instruction>>();
	}
	
	//This method should only return false if every single Queue is empty
	public void process() {
		//This method will go to all 3 from-q's and pull one off the top
		QItem q = this.fromProc.poll();
		if(q != null) {
			processFromProc(q);
		}
		q = this.fromData.poll();
		if(q != null) {
			processFromData(q);
		}
		q = this.fromL2.poll();
		if(q != null) {
			processFromL2(q);
		}
	}
	
	//Hit and valid: tell L1Data to give us the data
	//Miss or Invalid: check for a waiting line in case the data is already on its way for this address, 
	//otherwise tell L2C to give us the data
	private void processFromProc(QItem q) {
		Instruction instr = q.getInstruction();
		int instrAddress = instr.getAddress();
		int setNum = getSet(instrAddress);
		//Get the set that the address would be in if it is in cache
		ArrayList<ControllerEntry> set = this.sets.get(setNum);
		ControllerEntry matchingEntry = null;
		boolean foundMatch = false;
		for(ControllerEntry e : set) {
			if(instrAddress == e.getAddress()) {
				matchingEntry = e;
				foundMatch = true;
			}
		}
		if(!foundMatch) {
			for(ControllerEntry e : this.writeBuf) {
				if(instrAddress == e.getAddress() && e.isValid()) {
					matchingEntry = e;
					foundMatch = true;
					processInWriteBuf(q, e);
					return;
				}
			}
		}
		if(!foundMatch) {
			for(ControllerEntry e : this.victim) {
				if(instrAddress == e.getAddress() && e.isValid()) {
					matchingEntry = e;
					foundMatch = true;
					processInVictim(q, e);
					return;
				}
			}			
		}
		if(matchingEntry != null) {
			if(matchingEntry.isValid()) {
				//HIT
				QItem qu = new QItem(instr);
				this.toData.offer(qu);
				//If it is a write instruction and we have it then that entry is now dirty
				if(instr instanceof Write) {
					matchingEntry.setDirty(true);
				} else if(instr instanceof Eviction) {
					//Set valid, dirty and address for controller
					matchingEntry.setValid(false);
					matchingEntry.setDirty(false);
					matchingEntry.setLoc(Location.L1D);
					matchingEntry.setAddress(-1);
				}
			}
		} else {
			//MISS
			//Check if a waiting line already exists
			Queue<Instruction> waitingLine = this.instructionMisses.get(new Integer(instrAddress));
			if(waitingLine != null) {
				//There is already a line for this address, put the instruction in line
				waitingLine.offer(instr);
			} else {
				//There is no line for this, we need to tell L2 to give us the data and then create the line
				QItem qu = new QItem(instr);
				this.toL2.offer(qu);
				this.instructionMisses.put(instr.getAddress(), new LinkedList<Instruction>());
			}
			//TODO: Evict a cache line when the data comes back from L2
		}
	}
	
	//Method to process the data that comes back from L2C
	private void processFromL2(QItem q) {
		//TODO: Implement
		//If there is a read/write instruction coming from L2 that means we requested the data
		//So we need to put it into L1D meaning we have to evict a line from L1D from the corresponding set if that set is full
		
		//Will also need to process waiting lines if data is coming back for an address
		
		//This could also be an eviction from L2 sent to maintain mutual inclusion
		//In which case L1C would clear its values before passing it along to L1D
	}
	
	private void processFromData(QItem q) {
		//This is either:
		//1. Data coming back from a read instruction: pass it along to the processor
		//2. Data coming back from an eviction: pass it along to L2C so that it can process it
		Instruction instr = q.getInstruction();
		if(instr instanceof Read) {
			//It is data coming back from cache, send it to processor
			if(q.getData() != null) {
				this.toProc.offer(q);
			} else {
				System.out.println("ERROR: Read coming back from L1D to L1C did not contain data!");
			}
		} else if(instr instanceof Eviction) {
			Eviction e = (Eviction) instr;
			//TODO: If there is an eviction from data and clean = to victim, dirty = to write buffer
			//Only write buffer and victim send their evictions to L2
			//This means we need to create an eviction instruction to send to the WB or victim cache for a random line
			//Need to think about how to tell any of the caches that we need to write to one of their lines a new line 
			//(as in they do not have a matching address for it so they dont know where)
			if(q.getData() != null) {
				processL1DEviction(q);
				//this.toL2.offer(q);
			} else {
				System.out.println("ERROR: Eviction coming back from L1D to L1C did not contain data!");				
			}
		} else {
			System.out.println("ERROR: QItem coming back from L1D to L1C was not of type Read or Eviction!");
		}
	}
	
	//If L1D has sent an eviction then that eviction had to go through L1C first meaning that the L1C entry has already been cleared
	private void processL1DEviction(QItem q) {
		Eviction e = (Eviction) q.getInstruction();
		int instrAddress = e.getAddress();
		//Check if clean or dirty
		//Find the entry in L1C
		int setNum = getSet(instrAddress);
		ArrayList<ControllerEntry> set = this.sets.get(setNum);
		boolean valid = false;
		boolean dirty = true;
		ControllerEntry matchingEntry = null;
		for(ControllerEntry entry : set) {
			if(entry.getAddress() == instrAddress) {
				matchingEntry = entry;
			}
		}
		if(matchingEntry != null) {
			if(!matchingEntry.isValid()) {
				System.out.println("ERROR: When processing eviction from L1D in L1C the address to be evicted was invalid in L1C, not processing any further");
				return;
			}
			valid = true;
			dirty = matchingEntry.isDirty();
		} else {
			System.out.println("ERROR: When processing eviction from L1D to L1C, the address to evict was not found in L1C, not processing any further");
		}
		if(dirty) {
			//Move into WB
			//Check if there is an open space already, if no open spot pick one to evict
			for(int i = 0; i < this.bufVicSize; i++) {
				ControllerEntry entry = this.writeBuf.get(i);
				if(entry.getAddress() == -1) {
					//We have an open spot in WB, put it here
					//Entries in WB and Vic controllers should match up with the same spots in their memory arrays
					CacheEntry cacheE = this.writeBufData.get(i);
					if(cacheE.getAddress() != entry.getAddress()) {
						System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the WB controller and WB data, stopping process!");
						return;
					}
					//We have found a valid spot in the WB, place it here
					entry.setAddress(instrAddress);
					entry.setDirty(true);
					entry.setValid(true);
					entry.setLoc(Location.WRITE_BUFFER);
					cacheE.setAddress(instrAddress);
					cacheE.setData(e.getData().clone());
					return;
				}				
			}
			//Need to clear spot
			int evictionSpot = ThreadLocalRandom.current().nextInt(0, this.bufVicSize);
			ControllerEntry contrEntry = this.writeBuf.get(evictionSpot);
			CacheEntry cacheEntry = this.writeBufData.get(evictionSpot);
			if(contrEntry.getAddress() != cacheEntry.getAddress()) {
				System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the WB controller and WB data, stopping process!");
				return;
			}
			//Need to send eviction with new address and data
			Instruction newEviction = new Eviction(contrEntry.getAddress(), cacheEntry.getData().clone());
			QItem newQ = new QItem(newEviction);
			this.toL2.offer(newQ);
			//Need to set values of that cleared spot to the new address and data now there
			contrEntry.setAddress(instrAddress);
			contrEntry.setDirty(true);
			contrEntry.setValid(true);
			contrEntry.setLoc(Location.WRITE_BUFFER);
			cacheEntry.setAddress(instrAddress);
			cacheEntry.setData(e.getData().clone());
		} else {
			//Move into victim cache
			for(int i = 0; i < this.bufVicSize; i++) {
				ControllerEntry contrEntry = this.victim.get(i);
				if(contrEntry.getAddress() == -1) {
					//We have an open spot in WB, put it here
					//Entries in WB and Vic controllers should match up with the same spots in their memory arrays
					CacheEntry cacheEntry = this.victimData.get(i);
					if(cacheEntry.getAddress() != contrEntry.getAddress()) {
						System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the WB controller and WB data, stopping process!");
						return;
					}
					//We have found a valid spot in the WB, place it here
					contrEntry.setAddress(instrAddress);
					contrEntry.setDirty(false);
					contrEntry.setValid(true);
					contrEntry.setLoc(Location.WRITE_BUFFER);
					cacheEntry.setAddress(instrAddress);
					cacheEntry.setData(e.getData().clone());
					return;
				}
			}
		}
	}
	
	//Processes and instruction that needs an address that is currently in the write buffer
	//WARNING: Any changes in here you should double check processInVictim, duplicate code
	private void processInWriteBuf(QItem q, ControllerEntry controllerMatch) {
		Instruction instr = q.getInstruction();
		int instrAddr = instr.getAddress();
		CacheEntry dataMatch = null;
		for(CacheEntry e : this.writeBufData) {
			if(instrAddr == e.getAddress()) {
				dataMatch = e;
			}
		}
		if(dataMatch == null) {
			System.out.println("ERROR: Matching entry in WB Controller but not in WB Data!");
			return;
		}
		//Write: write to the entry, set dirty bit
		if(instr instanceof Write) {
			dataMatch.setData(((Write) instr).getData().clone());
			dataMatch.setAddress(instrAddr);
			controllerMatch.setAddress(instrAddr);
			controllerMatch.setDirty(true);
			controllerMatch.setValid(true);
			controllerMatch.setLoc(Location.WRITE_BUFFER);
			return;
		}
		//Read: Give it the data and pass it along to processor
		if(instr instanceof Read) {
			//Create copy of data to not pass ref to cache entry data
			q.setData(dataMatch.getData().clone());
			this.toProc.offer(q);
			return;
		}
		//Eviction: evict and pass to L2
		if(instr instanceof Eviction) {
			//If this buffer entry is clean, just wipe out the data, otherwise pass along eviction to L2 for write-back
			if(controllerMatch.isDirty()) {
				//Extract the data into the Eviction QItem so it can go to L2
				byte[] dataToEvict = dataMatch.getData().clone();
				q.setData(dataToEvict);
				this.toL2.offer(q);				
			}
			//Reset the data for this cache line
			controllerMatch.setAddress(-1);
			controllerMatch.setValid(false);
			controllerMatch.setDirty(false);
			controllerMatch.setLoc(Location.WRITE_BUFFER);
			dataMatch.setAddress(-1);
			dataMatch.setData(new byte[32]);
			return;
		}
	}
	
	//WARNING: Any changes in here you should double check processInVictim, duplicate code
	private void processInVictim(QItem q, ControllerEntry controllerMatch) {
		Instruction instr = q.getInstruction();
		int instrAddr = instr.getAddress();
		CacheEntry dataMatch = null;
		for(CacheEntry e : this.victimData) {
			if(instrAddr == e.getAddress()) {
				dataMatch = e;
			}
		}
		if(dataMatch == null) {
			System.out.println("ERROR: Matching entry in Victim Controller but not in Victim Data!");
			return;
		}
		//Write: Set valid, dirty, location and address for controllerMatch, set address and data for dataMatch
		if(instr instanceof Write) {
			dataMatch.setData(((Write) instr).getData().clone());
			dataMatch.setAddress(instrAddr);
			controllerMatch.setAddress(instrAddr);
			controllerMatch.setDirty(true);
			controllerMatch.setValid(true);
			controllerMatch.setLoc(Location.VICTIM);
			return;
		}
		//Read: Get the data from dataMatch, pass it along to processor
		if(instr instanceof Read) {
			//Create copy of data to not pass ref to cache entry data
			q.setData(dataMatch.getData().clone());
			this.toProc.offer(q);
			return;
		}
		//Eviction: If dirty, put in write buffer cache
		if(instr instanceof Eviction) {
			//If this buffer entry is clean, just wipe out the data, otherwise pass along eviction to L2 for write-back
			if(controllerMatch.isDirty()) {
				//Extract the data into the Eviction QItem so it can go to L2
				byte[] dataToEvict = dataMatch.getData().clone();
				q.setData(dataToEvict);
				this.toL2.offer(q);				
			}
			//Reset the data for this cache line
			controllerMatch.setAddress(-1);
			controllerMatch.setValid(false);
			controllerMatch.setDirty(false);
			controllerMatch.setLoc(Location.WRITE_BUFFER);
			dataMatch.setAddress(-1);
			dataMatch.setData(new byte[32]);
			return;
		}
	}
	
	private int getSet(int addr) {
		int setNum = addr % this.numberOfSets;
		return setNum;
	}
	
	public boolean areAnyLeft() {
		boolean result = false;
		if(this.toProc.size() > 0) {
			result = true;
		} else if(this.fromProc.size() > 0) {
			result = true;
		} else if(this.toData.size() > 0) {
			result = true;
		} else if(this.fromData.size() > 0) {
			result = true;
		} else if(this.toL2.size() > 0) {
			result = true;
		} else if(this.fromL2.size() > 0) {
			result = true;
		} else if(this.L2C.areAnyLeft()) {
			result = true;
		}
		return result;
	}
	
	public void printL1Cache() {
		System.out.println("Printing L1 Cache...");
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
	
	public void printL2Cache() {
		this.L2C.printCache();
	}
	
	public int getMemoryData(int addr) {
		return this.L2C.getMemoryData(addr);
	}
}
