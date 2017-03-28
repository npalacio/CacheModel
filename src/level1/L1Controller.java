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
import general.Put;
import general.QItem;
import general.Read;
import general.Write;
import level2.L2Controller;
import project2.BusAcks;
import project2.BusItem;
import project2.BusRequest;
import project2.BusWriteBack;
import project2.NodeQManager;
import project2.QManager;
import project2.State;

public class L1Controller {

	private StringBuilder sb;
	private List<ArrayList<ControllerEntry>> sets;
	private int numberOfSets = 128;
	private L1Data backingData;
	private NodeQManager qman;
	private Queue<BusRequest> busRequests = new LinkedList<BusRequest>();
	
	//Create a mapping of addresses to queues so that instructions going to the same address
	//that is not in the cache can all wait in line together for it
	private Map<Integer, Queue<Instruction>> waitingLines;
	
	//Since processor owns L1C it will pass in the queues to communicate with it
	//L1C initializes the other queues
	public L1Controller(StringBuilder stringB, NodeQManager qmanager) {

		this.sb = stringB;
		this.qman = qmanager;
				
		Initialize();
	}
	
	private void Initialize() {
		//Initialize L1Controller entries
		List<ArrayList<ControllerEntry>> newSets = new ArrayList<ArrayList<ControllerEntry>>(numberOfSets);
		for(int i = 0; i < this.numberOfSets; i++) {
			ArrayList<ControllerEntry> set = new ArrayList<ControllerEntry>(2);
			ControllerEntry entry0 = new ControllerEntry(State.INVALID, -1, new byte[32]);
			ControllerEntry entry1 = new ControllerEntry(State.INVALID, -1, new byte[32]);
			set.add(0, entry0);
			set.add(1, entry1);
			newSets.add(i, set);
		}
		sets = newSets;
		
		//Initialize waiting line for instructions that miss going to the same address
		this.waitingLines = new HashMap<Integer, Queue<Instruction>>();
	}
	
	public boolean Process() {
		boolean ret = false;
		if(ProcessFromResp()) {
			ret = true;
		}
		if(ProcessFromIC()) {
			ret = true;
		}
		if(ProcessAcks()) {
			ret = true;
		}
		return ret;
	}
	
	private boolean ProcessFromResp() {
		//TODO: Implement
		//if data/acks from request = either store the data or just process instructions for that address depending on what the request was
			//If request was for an upgrade
		//if request from node = put in q for processing at end (will do action then pass back to Resp)
		boolean ret = false;
		BusItem item = this.qman.Resp2L1CPull();
		while(item != null) {
			ret = true;
			if(item instanceof BusAcks) {
				ProcessBusAcks((BusAcks) item);
			} else if(item instanceof BusRequest) {
				ProcessBusRequest((BusRequest) item);
			}
		}
		return ret;
		//if: data that this node requested = pass to L1C Q
		//if: a request for an ack from another node = pass to L1C Q
	}
	
	private boolean ProcessFromIC() {
		//TODO: Implement
		//Load new instructions from IC
	}
	
	private boolean ProcessBusRequests() {
		//TODO: Implement
		//Service requests from other nodes
		
	}
	
	private void ProcessBusAcks(BusAcks acks) {
		//TODO: Make sure that we do not assume that we have this address because in between sending out this request
		//and getting it back we might have had to evict it
		//These are bus acks from the BC
		//Meaning we had a request that was filled, could be acks with data or just acks (meaning we already had the data)
		Integer address = acks.getAddress();
		byte[] data = acks.getData();
		State state = acks.getState();
		if(data != null) {
			StoreData(address, data, state);
		}
		//Here we need to process any instructions that were waiting on this address
		//If we already had the data and just needed an upgrade/acks that we can write then:
			//Change the state of the address
			//Process the waiting line of instructions
		UpdateState(address, state);
		ProcessWaitingInstructions(address);
	}
	
	private void ProcessBusRequest(BusRequest br) {
		//Just need to put it into a waiting line, will process this waiting line at end
		this.busRequests.offer(br);
	}

	private void ProcessWaitingInstructions(Integer address) {
		//TODO: Implement
		//Check if there are any waiting lines and try to resolve them
		//I may or may not have the necessary state to do what I want:
			//If I sent a BRead and got it back I can only read from that address
			//If there is a write instruction waiting in line I need to recognize that and send 
			//another bus request to upgrade
		Queue<Instruction> waitingLine = this.waitingLines.get(address);
		Instruction instr = waitingLine.poll();
		while(instr != null) {
			//Process instruction here
			ProcessInstruction(instr);
			//Grab next item
			instr = waitingLine.poll();
		}
	}
	
	private void ProcessInstruction(Instruction instr) {
		//TODO: Implement
		//This needs to handle everything
		Integer address = instr.getAddress();
		State addrState = GetState(address);
		if(instr instanceof Read) {
			Read r = (Read) instr;
			switch(addrState) {
				case EXCLUSIVE:
					Exclusive(r);
					break;
				case INVALID:
					Invalid(r);
					break;
				case MODIFIED:
					Modified(r);
					break;
				case SHARED:
					Shared(r);
					break;
				default:
					System.out.println("ERROR: State of instruction address is not caught in ProcessInstruction, returing");
					return;
			}
		} else if(instr instanceof Write) {
			Write w = (Write) instr;			
			switch(addrState) {
				case EXCLUSIVE:
					Exclusive(w);
					break;
				case INVALID:
					Invalid(w);
					break;
				case MODIFIED:
					Modified(w);
					break;
				case SHARED:
					Shared(w);
					break;
				default:
					System.out.println("ERROR: State of instruction address is not caught in ProcessInstruction, returing");
					return;
			}
		} else {
			System.out.println("ERROR: Inside L1C, Instruction was not instance of a Read or Write, returning");
			return;
		}
	}
	
	private void Exclusive(Read r) {
		//TODO: Implement
		//I have the most current version of this block, proceed with read
		ProcessRead(r);
	}
	private void Exclusive(Write w) {
		//TODO: Implement
		Integer address = w.getAddress();
		//I have the most current version of this block, proceed with write
		ProcessWrite(w);
		//Change state of entry to modified
		UpdateState(address, State.MODIFIED);
	}

	private void Invalid(Read r) {
		//TODO: Implement
		Integer address = r.getAddress();
		//I am missing this address, send a request (BusRead) to the bus for it
		SendOutBusRead(address);
		//Put this address into a waiting line
		PutInWaitingLine(r);
	}
	private void Invalid(Write w) {
		//TODO: Implement
		Integer address = w.getAddress();
		//I am missing this address, send a request (BusReadEx) to the bus for it
		SendOutBusReadEx(address);
		//Put this address into a waiting line
		PutInWaitingLine(w);
	}

	private void Modified(Read r) {
		//TODO: Implement
		//I have the most current version of this block, proceed with read
		ProcessRead(r);
	}
	private void Modified(Write w) {
		//TODO: Implement
		//I have the most current version of this block, proceed with write
		ProcessWrite(w);
	}

	private void Shared(Read r) {
		//TODO: Implement
		//I have the most current version of this block, proceed with read
		ProcessRead(r);
	}
	private void Shared(Write w) {
		//TODO: Implement
		Integer address = w.getAddress();
		//Send a request (BusUpgrade) to the bus for it
		SendOutBusUpgrade(address);
		//Put this address into a waiting line
		PutInWaitingLine(w);
	}
	
	private void ProcessRead(Read r) {
		//TODO: Implement
		//At this point we can assume we have the data in the correct state
		//We just need to finish processing the instruction and pass it to the node
	}

	private void ProcessWrite(Write r) {
		//TODO: Implement
		//At this point we can assume we have the data in the correct state
		//We just need to finish processing the instruction and pass it to the node
	}
	
	private void PutInWaitingLine(Instruction i) {
		//TODO: Implement
	}
	
	private void SendOutBusRead(Integer addr) {
		//TODO: Implement
		//Send out bus request for this address
	}

	private void SendOutBusReadEx(Integer addr) {
		//TODO: Implement
		//Send out bus request for this address
	}

	private void SendOutBusUpgrade(Integer addr) {
		//TODO: Implement
		//Send out bus request for this address
	}

	private void StoreData(Integer address, byte[] data, State state) {
		Integer setNum = getSet(address);
		List<ControllerEntry> set = this.sets.get(setNum);
		for(ControllerEntry e : set) {
			if(IsSetAvailable(e, address)) {
				//Store data here
				WriteEntry(e, address, data, state);
				return;
			}
		}
		//If we get here, the data has not been stored yet
		int chosenEntry = ThreadLocalRandom.current().nextInt(0, 2);
		ControllerEntry e = set.get(chosenEntry);
		FreeEntry(e);
		WriteEntry(e, address, data, state);
	}
	
	//Returns true if the address currently in the entry is -1 or
	//the same as the address we are trying to store there
	private boolean IsSetAvailable(ControllerEntry e, Integer dataAddress) {
		Integer addr = e.getAddress();
		if(addr == -1 || addr == dataAddress || e.getState() == State.INVALID) {
			return true;
		} else {
			return false;
		}
	}

	private void FreeEntry(ControllerEntry e) {
		//If this L1C gets an ackI request for an address that it requested to write back, then it needs
		//to write that address back/give the asking node the new data for that address
		if(e.getAddress() == -1) {
			System.out.println("WARNING: In L1C, trying to free ControllerEntry that already has no address stored there, returning");
			return;
		}
		Integer address = e.getAddress();
		byte[] dataToWriteBack = e.getData().clone();
		BusItem bwb = new BusWriteBack(address, dataToWriteBack);
		WriteEntry(e, -1, new byte[32], State.INVALID);
		//TODO: Figure out how to synchronize/where to send this
	}
	
	//TODO:
	//What if we send out a request for an address and then right after that we receive a request for that same address from
	//another node?
	//This would not happen because we would not receive anything other than what we requested until after we got our request back
	//since we own the bus that whole time
	//This could happen if we have a request waiting for the bus meanwhile we service another request for that same address
	//and invalidate our own address
	//Solution: every request we send out needs to come back with data and write backs need to propagate immediately from a
	//node to the BC/Memory as soon as we realize we need it (but wouldnt that already happen since we would send a request
	//from another node with the data that we have thats modified?)
	
	private void UpdateState(Integer address, State state) {
		ControllerEntry e = FindEntry(address);
		if(e != null) {
			//Update only the state for this entry
			byte[] sameData = e.getData().clone();
			WriteEntry(e, address, sameData, state);
		} else {
			System.out.println("ERROR: In L1C, in UpdateState, could not find the controller entry that we are trying to update, returning");
		}
	}
	
	private void WriteEntry(ControllerEntry e, Integer addr, byte[] data, State s) {
		e.setAddress(addr);
		e.setData(data);
		e.setState(s);
	}
	
	private ControllerEntry FindEntry(Integer address) {
		Integer setNum = getSet(address);
		List<ControllerEntry> set = this.sets.get(setNum);
		for(ControllerEntry e : set) {
			Integer entryAddr = e.getAddress();
			if(entryAddr == address) {
				return e;
			}
		}
		return null;
	}
	
	private State GetState(Integer addr) {
		Integer setNum = getSet(addr);
		ArrayList<ControllerEntry> set = this.sets.get(setNum);
		for(ControllerEntry e : set) {
			if(e.getAddress() == addr) {
				return e.getState();
			}
		}
		return State.INVALID;
	}
	
	private int getSet(int addr) {
		int setNum = addr % this.numberOfSets;
		return setNum;
	}
	
//	private void processFromData(QItem q) {
//		//This is either:
//		//1. Data coming back from a read instruction: pass it along to the processor
//		//2. Data coming back from an eviction: put it in WB or Victim
//		Instruction instr = q.getInstruction();
//		if(instr instanceof Read) {
//			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L1D to L1C: Data from address " + instr.getAddress());
//			//It is data coming back from cache, send it to processor
//			if(q.getData() != null) {
//				this.toInstrCache.offer(q);
//			} else {
//				System.out.println("ERROR: Read coming back from L1D to L1C did not contain data!");
//			}
//		} else if(instr instanceof Write) {
//			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L1D to L1C: Wrote to address " + instr.getAddress());
//			this.toInstrCache.offer(q);
//		} else if(instr instanceof Eviction) {
//			processL1DEviction(q);
////		} else if(instr instanc) {
//			
//		} else {
//			System.out.println("ERROR: QItem coming back from L1D to L1C was not of type Read or Eviction!");
//		}
//	}
//	
	
//	public boolean areAnyLeft() {
//		boolean result = false;
//		if(this.fromInstrCache.size() > 0) {
//			result = true;
//		} else if(this.toData.size() > 0) {
//			result = true;
//		} else if(this.fromData.size() > 0) {
//			result = true;
//		}
//		return result;
//	}
//	
//	public void printL1Cache() {
//		System.out.println("Printing L1 Cache...");
//		if(sets == null) {
//			System.out.println("\tL1C not initialized!");
//			return;
//		}
//		int i = 0;
//		for(List<ControllerEntry> set : this.sets) {
//			System.out.println("Set " + i);
//			int j = 0;
//			for(ControllerEntry entry : set) {
//				System.out.println("\tEntry " + j);
//				int L1CAddr = entry.getAddress();
//				CacheEntry dataEntry = backingData.getSets().get(i).get(j);
//				int L1DAddr = dataEntry.getAddress();
//				if(L1CAddr == L1DAddr) {
//					System.out.println("\t\tAddress = " + L1CAddr + ", data = " + java.nio.ByteBuffer.wrap(dataEntry.getData()).getInt() + 
//									   ", isValid = " + entry.isValid() + ", isDirty = " + entry.isDirty());
//				} else {
//					System.out.println("ERROR: Controller address = " + L1CAddr + ", Data address = " + L1DAddr);
//				}
//				j++;
//			}
//			i++;
//		}
//	}
//	//Hit and valid: tell L1Data to give us the data
//	//Miss or Invalid: check for a waiting line in case the data is already on its way for this address, 
//	//otherwise tell L2C to give us the data
//	//This will always only be a read or a write
//	private void processFromProc(QItem q) {
//		Instruction instr = q.getInstruction();
//		int instrAddress = instr.getAddress();
//		System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ": Instruction Cache to L1C");
//		int setNum = getSet(instrAddress);
//		//Get the set that the address would be in if it is in cache
//		ArrayList<ControllerEntry> set = this.sets.get(setNum);
//		ControllerEntry matchingEntry = null;
//		boolean foundMatch = false;
//		for(ControllerEntry e : set) {
//			if(instrAddress == e.getAddress()) {
//				matchingEntry = e;
//				foundMatch = true;
//			}
//		}
////		if(!foundMatch) {
////			for(ControllerEntry e : this.writeBuf) {
////				if(instrAddress == e.getAddress() && e.isValid()) {
////					matchingEntry = e;
////					foundMatch = true;
////					processInWriteBuf(q, e);
////					return;
////				}
////			}
////		}
////		if(!foundMatch) {
////			for(ControllerEntry e : this.victim) {
////				if(instrAddress == e.getAddress() && e.isValid()) {
////					matchingEntry = e;
////					foundMatch = true;
////					processInVictim(q, e);
////					return;
////				}
////			}			
////		}
//		if(matchingEntry != null) {
//			if(matchingEntry.isValid()) {
//				//HIT
//				System.out.println("Instruction " + instr.getNumber() + ", HIT in L1, fetching from L1D");
//				QItem qu = new QItem(instr);
//				this.toData.offer(qu);
//				//If it is a write instruction and we have it then that entry is now dirty
//				if(instr instanceof Write) {
//					matchingEntry.setDirty(true);
//				} else if(instr instanceof Eviction) {
//					//Set valid, dirty and address for controller
//					matchingEntry.setValid(false);
//					matchingEntry.setDirty(false);
//					matchingEntry.setLoc(Location.L1D);
//					matchingEntry.setAddress(-1);
//				}
//			}
//		} else {
//			//MISS
//			//Check if a waiting line already exists
//			Queue<Instruction> waitingLine = this.instructionMisses.get(new Integer(instrAddress));
//			if(waitingLine != null) {
//				//There is already a line for this address, put the instruction in line
//				System.out.println("Instruction " + instr.getNumber() + ", MISS in L1C, Data is either being retrieved from L2 or evicted from L1D and will be in L1 WB or L1 Victim cache");
//				waitingLine.offer(instr);
//			} else {
//				//There is no line for this, we need to tell L2 to give us the data and then create the line
////				System.out.println("Instruction " + instr.getNumber() + ", MISS in L1C, retrieving from L2");
////				QItem qu = new QItem(instr);
////				this.toL2.offer(qu);
////				this.instructionMisses.put(instr.getAddress(), new LinkedList<Instruction>());
//				//TODO: I think this is where I issue a bus request for the data?
//			}
//		}
//	}
	
	//Method to process the data that comes back from L2C
//	private void processFromL2(QItem q) {						
//		Instruction instr = q.getInstruction();
//		int instrAddress = instr.getAddress();
//		int setNum = getSet(instrAddress);
//		ArrayList<ControllerEntry> set = this.sets.get(setNum);
//		if(instr instanceof Read || instr instanceof Write) {
//			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", L2C to L1C: Data from address " + instr.getAddress());
//			//Check if a spot in the set is open
//			ControllerEntry entryToBeOverwritten = null;
//			for(ControllerEntry entry : set) {
//				if(entry.getAddress() == -1) {
//					//This spot is open
//					entryToBeOverwritten = entry;
//				}
//			}
//			if(entryToBeOverwritten == null) {
//				//Pick one of the two entries in the set to evict
//				int chosenEntry = ThreadLocalRandom.current().nextInt(0, 2);
//				entryToBeOverwritten = set.get(chosenEntry);
//				//Create eviction and set dirty bit
//				int evictionAddress = entryToBeOverwritten.getAddress();
//				Eviction eviction = new Eviction(evictionAddress);
//				eviction.setDirty(entryToBeOverwritten.isDirty());
//				QItem qEviction = new QItem(eviction);
//				this.toData.offer(qEviction);
//				System.out.println("L1C: Evicting address " + evictionAddress + " from L1D to make room for address " + instrAddress);
//				//Need to create waiting line for anything that is looking for this data since we sent the eviction
//				//We wont have this data until the eviction comes back from L1D
//				this.instructionMisses.put(evictionAddress, new LinkedList<Instruction>());
//			}
//			//We have an open spot now for sure (entryToBeOverwritten)
//			entryToBeOverwritten.setAddress(instrAddress);
//			entryToBeOverwritten.setValid(true);
//			entryToBeOverwritten.setDirty(q.isDataDirty());
//			entryToBeOverwritten.setLoc(Location.L1D);
//			//The QItem will be holding the data that is returned from L2
//			Put putInstr = new Put(instrAddress, q.getData().clone());
//			//L1Data will get address and data from put instruction
//			QItem putItem = new QItem(putInstr);
//			this.toData.offer(putItem);
//			//Now the L1Data will have evicted the necessary line and put in the new data in that line,
//			//we can process the original instruction (In this case a read or write)
//			//If it is a write instruction then our line is going to be dirty
//			if(instr instanceof Write) {
//				entryToBeOverwritten.setDirty(true);
//			}
//			//We can reuse same QItem, just clear the data so it looks like any other QItem coming to L1Data
//			q.setData(null);
//			this.toData.offer(q);
//			//Process the other instructions that were waiting on this data, if any
//			Queue<Instruction> waitingLine = instructionMisses.get(instrAddress);
//			if(waitingLine != null) {
//				for(Instruction waitingInstr : waitingLine) {
//					QItem q1 = new QItem(waitingInstr);
//					this.toData.offer(q1);
//					if(waitingInstr instanceof Write) {
//						entryToBeOverwritten.setDirty(true);
//					}
//				}
//				//Delete the waiting line since we got the data back and it is processed
//				instructionMisses.put(instrAddress, null);
//			}
//		}
//		
//		//An eviction from L2 only comes down due to violation of mutual inclusion
//		//Be sure to clear L1C entry before sending to L1D
//		else if(instr instanceof Eviction) {
//			System.out.println("L1: Evicting address " + instr.getAddress() + " in order to maintain mutual inclusion");
//			//Make sure fromL2 boolean is set
//			Eviction e = (Eviction) instr;
//			if(e.isFromL2ToL1()) {
//				//Correct bit has been set
//				//Make sure the set has the address to be evicted, then reset the L1C entry and pass the eviction on to L1D
//				boolean entryFound = false;
//				ControllerEntry contrEntry = null;
//				for(ControllerEntry entry : set) {
//					if(instrAddress == entry.getAddress()) {
//						contrEntry = entry;
//						entryFound = true;
//						if(!contrEntry.isValid()) {
//							System.out.println("ERROR: L1C is processing eviction from L2 but the matching address in L1C is not valid in L1, stopping process!");
//							return;
//						}
//						e.setDirty(contrEntry.isDirty());
//						this.toData.offer(q);
//						contrEntry.setAddress(-1);
//						contrEntry.setDirty(false);
//						contrEntry.setValid(false);
//						contrEntry.setLoc(Location.L1D);
//					}
//				}
//				//Not found in L1C Check WB and Victim caches
//				if(!entryFound) {
//					for(ControllerEntry entry : this.writeBuf) {
//						if(entry.getAddress() ==  instrAddress) {
//							if(!entry.isValid()) {
//								System.out.println("ERROR: L1C is processing eviction from L2 but the matching address in WB is not valid in L1, stopping process!");
//								return;								
//							}
//							entryFound = true;
//							if(entry.isDirty()) {
//								//Entry is dirty, need to pass to L2C for write back
//								int entryIndex = this.writeBuf.indexOf(entry);
//								CacheEntry dataEntry = this.writeBufData.get(entryIndex);
//								if(dataEntry.getAddress() != instrAddress) {
//									System.out.println("ERROR: Mismatching addresses across WB controller and data, stopping process!");
//									return;
//								}
//								e.setData(dataEntry.getData().clone());
//								this.toL2.offer(q);
//								entry.setAddress(-1);
//								entry.setDirty(false);
//								entry.setValid(false);
//								entry.setLoc(Location.WRITE_BUFFER);
//								dataEntry.setAddress(-1);
//								dataEntry.setData(new byte[32]);
//								return;
//							} else {
//								System.out.println("WARNING: Entry in L1 WB that is not dirty, continuing process");
//								int entryIndex = this.writeBuf.indexOf(entry);
//								CacheEntry dataEntry = this.writeBufData.get(entryIndex);
//								if(dataEntry.getAddress() != instrAddress) {
//									System.out.println("ERROR: Mismatching addresses across WB controller and data, stopping process!");
//									return;
//								}
//								entry.setAddress(-1);
//								entry.setDirty(false);
//								entry.setValid(false);
//								entry.setLoc(Location.WRITE_BUFFER);
//								dataEntry.setAddress(-1);
//								dataEntry.setData(new byte[32]);
//								return;
//							}
//						}
//						
//					}
//					for(ControllerEntry entry : this.victim) {
//						if(entry.getAddress() ==  instrAddress) {
//							if(!entry.isValid()) {
//								System.out.println("ERROR: L1C is processing eviction from L2 but the matching address in Victim is not valid in L1, stopping process!");
//								return;								
//							}
//							entryFound = true;
//							if(!entry.isDirty()) {
//								int entryIndex = this.victim.indexOf(entry);
//								CacheEntry dataEntry = this.victimData.get(entryIndex);
//								if(dataEntry.getAddress() != instrAddress) {
//									System.out.println("ERROR: Mismatching addresses across victim controller and data, stopping process!");
//									return;
//								}
//								entry.setAddress(-1);
//								entry.setDirty(false);
//								entry.setValid(false);
//								entry.setLoc(Location.WRITE_BUFFER);
//								dataEntry.setAddress(-1);
//								dataEntry.setData(new byte[32]);
//								return;
//							} else {
//								//Entry is dirty, need to pass to L2C for write back
//								System.out.println("WARNING: Entry in L1 Victim that is dirty, continuing process");
//								int entryIndex = this.victim.indexOf(entry);
//								CacheEntry dataEntry = this.victimData.get(entryIndex);
//								if(dataEntry.getAddress() != instrAddress) {
//									System.out.println("ERROR: Mismatching addresses across victim controller and data, stopping process!");
//									return;
//								}
//								e.setData(dataEntry.getData().clone());
//								this.toL2.offer(q);
//								entry.setAddress(-1);
//								entry.setDirty(false);
//								entry.setValid(false);
//								entry.setLoc(Location.WRITE_BUFFER);
//								dataEntry.setAddress(-1);
//								dataEntry.setData(new byte[32]);
//								return;
//							}
//						}
//					}
//				}
//				if(contrEntry == null) {
//					//This is assuming that we are looking in the right set
//					System.out.println("ERROR: L2C sent an eviction to L1C but L1C/WB/Vic does not have the address to be evicted, stopping process!");
//					return;
//				}
//			} else {
//				System.out.println("ERROR: L2C sent L1C an eviction but did not set the L2ToL1 bit, stopping process!");
//				return;
//			}
//			//L1D only
//		} else {
//			System.out.println("ERROR: L2C sent L1C something other than a read, write or eviction instruction!");
//		}
//	}

	//If L1D has sent an eviction then that eviction had to go through L1C first meaning that the L1C entry has already been cleared
	//This puts the evicted line from L1D in either WB or Victim cache depending on if it is dirty or not
//	private void processL1DEviction(QItem q) {
		//TODO: Figure out new way to process eviction from L1Data
//		Eviction e = (Eviction) q.getInstruction();
//		int instrAddress = e.getAddress();
//		//If it came from L2, just pass along to L2
//		if(e.isFromL2ToL1()) {
//			System.out.println("L1C: L1D evicted data from address " + instrAddress + " in order to maintain mutual exclusion, passing back to L2C");
//			this.toL2.offer(q);
//			return;
//		}
//		//Check if clean or dirty
//		if(e.isDirty()) {
//			//Move into WB
//			System.out.println("L1D to L1C: Evict address " + instrAddress + " to L1 Write Buffer");
//			//Check if there is an open space already, if no open spot pick one to evict
//			for(int i = 0; i < this.bufVicSize; i++) {
//				ControllerEntry entry = this.writeBuf.get(i);
//				if(entry.getAddress() == -1) {
//					//We have an open spot in WB, put it here
//					//Entries in WB and Vic controllers should match up with the same spots in their memory arrays
//					CacheEntry cacheE = this.writeBufData.get(i);
//					if(cacheE.getAddress() != entry.getAddress()) {
//						System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the WB controller and WB data, stopping process!");
//						return;
//					}
//					//We have found a valid spot in the WB, place it here
//					entry.setAddress(instrAddress);
//					entry.setDirty(true);
//					entry.setValid(true);
//					entry.setLoc(Location.WRITE_BUFFER);
//					cacheE.setAddress(instrAddress);
//					cacheE.setData(e.getData().clone());
//					Queue<Instruction> waitingLine = this.instructionMisses.get(new Integer(instrAddress));
//					if(waitingLine != null) {
//						for(Instruction instr : waitingLine) {
//							processInWriteBuf(new QItem(instr), entry);
//						}
//					}
//					return;
//				}				
//			}
			
			//Need to clear spot
//			int evictionSpot = ThreadLocalRandom.current().nextInt(0, this.bufVicSize);
//			ControllerEntry contrEntry = this.writeBuf.get(evictionSpot);
//			CacheEntry cacheEntry = this.writeBufData.get(evictionSpot);
//			if(contrEntry.getAddress() != cacheEntry.getAddress()) {
//				System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the WB controller and WB data, stopping process!");
//				return;
//			}
//			
//			if(!contrEntry.isDirty()){
//				//Need to send eviction with new address and data
//				System.out.println("L1C to L2C: Evicting address " + contrEntry.getAddress() + " to L2 to make room in Write Buffer");
//				Instruction newEviction = new Eviction(contrEntry.getAddress(), cacheEntry.getData().clone());
//				QItem newQ = new QItem(newEviction);
//				this.toL2.offer(newQ);
//			} else {
//				System.out.println("WARNING: When processing L1D eviction entry in WB that was not dirty, continuing process");
//			}
//			
//			//Need to set values of that cleared spot to the new address and data now there
//			contrEntry.setAddress(instrAddress);
//			contrEntry.setDirty(true);
//			contrEntry.setValid(true);
//			contrEntry.setLoc(Location.WRITE_BUFFER);
//			cacheEntry.setAddress(instrAddress);
//			cacheEntry.setData(e.getData().clone());
//			Queue<Instruction> waitingLine = this.instructionMisses.get(new Integer(instrAddress));
//			if(waitingLine != null) {
//				for(Instruction instr : waitingLine) {
//					processInWriteBuf(new QItem(instr), contrEntry);
//				}
//			}
//			
//		} else {
//			//Move into victim cache
//			System.out.println("L1D to L1C: Evict address " + instrAddress + " to L1 Victim Cache");
//			for(int i = 0; i < this.bufVicSize; i++) {
//				ControllerEntry contrEntry = this.victim.get(i);
//				if(contrEntry.getAddress() == -1) {
//					//We have an open spot in WB, put it here
//					//Entries in WB and Vic controllers should match up with the same spots in their memory arrays
//					CacheEntry cacheEntry = this.victimData.get(i);
//					if(cacheEntry.getAddress() != contrEntry.getAddress()) {
//						System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the Victim controller and Victim data, stopping process!");
//						return;
//					}
//					//We have found a valid spot in the Victim cache, place it here
//					contrEntry.setAddress(instrAddress);
//					contrEntry.setDirty(false);
//					contrEntry.setValid(true);
//					contrEntry.setLoc(Location.VICTIM);
//					cacheEntry.setAddress(instrAddress);
//					cacheEntry.setData(e.getData().clone());
//					Queue<Instruction> waitingLine = this.instructionMisses.get(new Integer(instrAddress));
//					if(waitingLine != null) {
//						for(Instruction instr : waitingLine) {
//							processInVictim(new QItem(instr), contrEntry);
//						}
//					}
//					return;
//				}
//			}
//
//			//Need to clear spot
//			int evictionSpot = ThreadLocalRandom.current().nextInt(0, this.bufVicSize);
//			ControllerEntry contrEntry = this.victim.get(evictionSpot);
//			CacheEntry cacheEntry = this.victimData.get(evictionSpot);
//			if(contrEntry.getAddress() != cacheEntry.getAddress()) {
//				System.out.println("ERROR: (In processing eviction from L1D) Mismatching addresses across entries in the WB controller and WB data, stopping process!");
//				return;
//			}
//			
//			//Need to send eviction with new address and data if this entry we are kicking out is dirty
//			if(contrEntry.isDirty()){
//				System.out.println("L1C to L2C: Evicting address " + contrEntry.getAddress() + " to L2 to make room in Victim cache");
//				Instruction newEviction = new Eviction(contrEntry.getAddress(), cacheEntry.getData().clone());
//				QItem newQ = new QItem(newEviction);
//				this.toL2.offer(newQ);
//			}
//			
//			//Need to set values of that cleared spot to the new address and data now there
//			contrEntry.setAddress(instrAddress);
//			contrEntry.setDirty(true);
//			contrEntry.setValid(true);
//			contrEntry.setLoc(Location.VICTIM);
//			cacheEntry.setAddress(instrAddress);
//			cacheEntry.setData(e.getData().clone());
//			Queue<Instruction> waitingLine = this.instructionMisses.get(new Integer(instrAddress));
//			if(waitingLine != null) {
//				for(Instruction instr : waitingLine) {
//					processInVictim(new QItem(instr), contrEntry);
//				}
//			}
//		}
//	}
	
	//Processes an instruction that needs an address that is currently in the write buffer
	//WARNING: Any changes in here you should double check processInVictim, duplicate code
//	private void processInWriteBuf(QItem q, ControllerEntry controllerMatch) {
//		Instruction instr = q.getInstruction();
//		System.out.println("Instruction " + instr.getNumber() + ", HIT in L1, fetching from Write Buffer");
//		int instrAddr = instr.getAddress();
//		int entryIndex = this.writeBuf.indexOf(controllerMatch);
//		CacheEntry dataMatch = this.writeBufData.get(entryIndex);
//		if(dataMatch == null) {
//			System.out.println("ERROR: Matching entry in WB Controller but not in WB Data!");
//			return;
//		}
//		if(dataMatch.getAddress() != controllerMatch.getAddress()) {
//			System.out.println("ERROR: When processing instr in WB, address of controller entry does not match that of cache entry, stopping process!");
//			return;
//		}
//		//Write: write to the entry, set dirty bit
//		if(instr instanceof Write) {
//			dataMatch.setData(((Write) instr).getData().clone());
//			dataMatch.setAddress(instrAddr);
//			controllerMatch.setAddress(instrAddr);
//			controllerMatch.setDirty(true);
//			controllerMatch.setValid(true);
//			controllerMatch.setLoc(Location.WRITE_BUFFER);
//			return;
//		}
//		//Read: Give it the data and pass it along to processor
//		if(instr instanceof Read) {
//			//Create copy of data to not pass ref to cache entry data
//			q.setData(dataMatch.getData().clone());
//			this.toInstrCache.offer(q);
//			return;
//		}
//		//Eviction: evict and pass to L2
//		//I dont think this will ever come from the processor
//		if(instr instanceof Eviction) {
//			//If this buffer entry is clean, just wipe out the data, otherwise pass along eviction to L2 for write-back
//			if(controllerMatch.isDirty()) {
//				//Extract the data into the Eviction QItem so it can go to L2
//				byte[] dataToEvict = dataMatch.getData().clone();
//				((Eviction) instr).setData(dataToEvict);
//				this.toL2.offer(q);				
//			}
//			//Reset the data for this cache line
//			controllerMatch.setAddress(-1);
//			controllerMatch.setValid(false);
//			controllerMatch.setDirty(false);
//			controllerMatch.setLoc(Location.WRITE_BUFFER);
//			dataMatch.setAddress(-1);
//			dataMatch.setData(new byte[32]);
//		}
//	}
	
	//WARNING: Any changes in here you should double check processInVictim, duplicate code
//	private void processInVictim(QItem q, ControllerEntry controllerMatch) {
//		Instruction instr = q.getInstruction();
//		System.out.println("Instruction " + instr.getNumber() + ", HIT in L1, fetching from Victime Cache");
//		int instrAddr = instr.getAddress();
//		int entryIndex = this.victim.indexOf(controllerMatch);
//		CacheEntry dataMatch = this.victimData.get(entryIndex);
//		if(dataMatch == null) {
//			System.out.println("ERROR: Matching entry in Victim Controller but not in Victim Data!");
//			return;
//		}
//		if(dataMatch.getAddress() != controllerMatch.getAddress()) {
//			System.out.println("ERROR: When processing instr in Victim, address of controller entry does not match that of cache entry, stopping process!");
//			return;
//		}
//		//Write: Set valid, dirty, location and address for controllerMatch, set address and data for dataMatch
//		if(instr instanceof Write) {
//			dataMatch.setData(((Write) instr).getData().clone());
//			dataMatch.setAddress(instrAddr);
//			controllerMatch.setAddress(instrAddr);
//			controllerMatch.setDirty(true);
//			controllerMatch.setValid(true);
//			controllerMatch.setLoc(Location.VICTIM);
//			return;
//		}
//		//Read: Get the data from dataMatch, pass it along to processor
//		if(instr instanceof Read) {
//			//Create copy of data to not pass ref to cache entry data
//			q.setData(dataMatch.getData().clone());
//			this.toInstrCache.offer(q);
//			return;
//		}
//		//Eviction: If dirty, put in write buffer cache
//		if(instr instanceof Eviction) {
//			//If this buffer entry is clean, just wipe out the data, otherwise pass along eviction to L2 for write-back
//			if(controllerMatch.isDirty()) {
//				//Extract the data into the Eviction QItem so it can go to L2
//				byte[] dataToEvict = dataMatch.getData().clone();
//				((Eviction) instr).setData(dataToEvict);
//				this.toL2.offer(q);				
//			}
//			//Reset the data for this cache line
//			controllerMatch.setAddress(-1);
//			controllerMatch.setValid(false);
//			controllerMatch.setDirty(false);
//			controllerMatch.setLoc(Location.WRITE_BUFFER);
//			dataMatch.setAddress(-1);
//			dataMatch.setData(new byte[32]);
//		}
//	}
	

	
//	public void printL2Cache() {
//		this.L2C.printCache();
//	}
//	
//	public int getMemoryData(int addr) {
//		return this.L2C.getMemoryData(addr);
//	}
}
