package project2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import general.Instruction;
import general.Memory;
import general.Read;
import general.Write;

public class BusController {

	//Communications
	private QManager qman;

	//Nodes
	private List<Node> nodes = new ArrayList<Node>();
	private Integer numberOfNodes = -1;
	private List<List<Instruction>> nodeInstructions;
	private boolean priorityMode;
	
	//State of bus
	private Integer busOwner = -1;
	private Integer prevBusOwner = -1;
	private Integer busTransCount = 0;
	
	//State of request
	private Integer responseNum = 0;
	private Integer busReqAddr;
	private byte[] dataForRequest;
	private RequestType typeOfBusRequest;
	private boolean acksReady = false;
	
	//State of processing instructions
	private Integer cycleCount = 0;
	private String outputFolder;
	
	//Memory
	private Integer memSize = 131072;
	private Memory memory;
		
	public static void main(String[] args) {
		BusController bc = new BusController();
		bc.Initialize(args);
	}
	
	private void Initialize(String[] args) {
//		String fileName = args[0];
		String fileName = "input/P2.1Node.SingleRead.txt";
		//-1 implies no one owns the bus
		InitializeMemory();
//		InitializeNodes();
		ProcessInstructions(fileName);
	}
	
	private void InitializeMemory() {
		this.memory = new Memory(this.memSize);
	}
	
	private void ProcessInstructions(String fileName) {
		FileInputStream fis = null;
		BufferedReader reader = null;
		System.out.println("Inside ProcessInstructions");
		try {
			fis = new FileInputStream(fileName);
			reader = new BufferedReader(new InputStreamReader(fis));
			String line = reader.readLine();
			int i = 0;
			while(line != null) {
				if(i == 0) {
					//This should tell us the number of nodes and if we are in priorityMode or not
					ProcessFirstLine(line);
				} else if(line.equals(";")) {
					//We have our bath to send
					SendInstructionBatch();
				} else {
					String[] tokens = line.split(" ");
					Instruction instr = null;
					int node = -1;
					if(tokens[1].equals("read") && tokens.length == 3) {
						node = Integer.parseInt(tokens[0]);
						instr = new Read(i, Integer.parseInt(tokens[2]), node);
					} else if(tokens[1].equals("read") && tokens.length == 4) {
						node = Integer.parseInt(tokens[0]);
						instr = new Read(i, Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), node);
					} else if(tokens[1].equals("write") && tokens.length == 4) {
						node = Integer.parseInt(tokens[0]);
						instr = new Write(i, Integer.parseInt(tokens[2]), ByteBuffer.allocate(32).putInt(Integer.parseInt(tokens[3])).array(), node);
					} else {
						System.out.println("Invalid input for instruction");
					}
					//Add the instruction to the corresponding node
					System.out.println("Finished grabbing instruction");
					System.out.println("Instruction list size = " + this.nodeInstructions.size());
					this.nodeInstructions.get(node).add(instr);
				}
				i++;
				line = reader.readLine();
			}
			System.out.println("Done reading instructions");
			FinishProcessing();
		} catch (Exception e) {
//			System.out.println();
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch(IOException e) {
				System.out.println(e);
			}
		}
	}

	private void ProcessFirstLine(String line) {
		System.out.println("Processing first line");
		String[] tokens = line.split(" ");
		this.numberOfNodes = Integer.parseInt(tokens[0]);
		this.priorityMode = tokens[1].equals("true");
		this.outputFolder = "output" + File.separator + tokens[2];
		if(this.numberOfNodes > 3) {
			System.out.println("WARNING: More than 3 nodes passed in (" + this.numberOfNodes + ")");
		}
		InitializeNodes();
	}

	private void SendInstructionBatch() {
		System.out.println("Sending instruction batch");
		for(int i = 0; i < this.numberOfNodes; i++) {
			List<Instruction> instructions = this.nodeInstructions.get(i);
			Node n = this.nodes.get(i);
			n.AddInstructions(instructions);
		}
//		this.nodeInstructions = new ArrayList<List<Instruction>>(this.numberOfNodes);
		InitializeInstructionLists();
		//Call process
		Process();
	}

	private void InitializeNodes() {
		System.out.println("Creating instruction lists for " + this.numberOfNodes + " nodes");
		InitializeInstructionLists();
		System.out.println("Size of lists of instructions = " + this.nodeInstructions.size());
		this.qman = new QManager(this.numberOfNodes);
		for(int i = 0; i < this.numberOfNodes; i++) {
			CreateNode(i);
		}
	}

	private void CreateNode(int i) {
		Node node = new Node(i, this.qman, this.priorityMode, this.outputFolder);
		this.nodes.add(i, node);
	}
	
	private void InitializeInstructionLists() {
		this.nodeInstructions = new ArrayList<List<Instruction>>(this.numberOfNodes);
		for(int i = 0; i < this.numberOfNodes; i++) {
			List<Instruction> instructions = new ArrayList<Instruction>();
			this.nodeInstructions.add(i, instructions);
		}
	}
	
	private void FinishProcessing() {
		while(Process()) {
			//Keep going
		}
		//We are done
		TerminateNodes();
		System.out.println("Finished processing all instructions!");
	}
	
	private void TerminateNodes() {
		for(Node n : this.nodes) {
			n.Terminate();
		}
	}
	
	private boolean Process() {
		//Every cycle we can only do 1 of these things:
			//Broadcast a request for a node
			//Send data to a node (if all acks are in)
			//grab an acknowledgement for a node
		//Check if anyone needs the bus (if no one currently owns it) and grant them the bus
		//If someone got the bus, broadcast their request if necessary
		System.out.println("*************************************");
		System.out.println("CYCLE " + this.cycleCount);
		System.out.println("*************************************");
		boolean notDone = false;
		BusItem item = this.qman.WriteBackPull();
		if(item != null) {
			if(item instanceof BusWriteBack) {
				ProcessWriteBack((BusWriteBack) item);
			} else {
				System.out.println("ERROR: Item not instance of BusWriteBack in BC.Process");
			}
		} else if(this.acksReady) {
			//Acks are all in, pass data/acks
			PassDataToOwner();
			if(ProcessNodes()) {
				notDone = true;
			}
		} else if(this.busOwner == -1) {
			//No one owns the bus, try to give it to someone
			TryGrantBus();
			if(ProcessNodes()) {
				notDone = true;
			}
		} else {
			//Waiting on acks, someone currently owns the bus
			if(ProcessNodes()) {
				notDone = true;
			}
			TryGrabAcks();
		}		
		this.cycleCount++;
		//If any nodes are still processing we are not done
		//If there is a bus owner still we are not done (waiting on acks)
		boolean ret = notDone || (this.busOwner != -1) || this.qman.AreAnyLeft();
		return ret;
	}
	
	private void ProcessWriteBack(BusWriteBack wb) {
		Integer address = wb.getAddress();
		byte[] data = wb.getData();
		if(data != null) {
			this.memory.setData(address, data);
		} else {
			System.out.println("ERROR: In BC.ProcessWriteBack, data from WB was null, returning");
			return;
		}
		System.out.println("BC: Wrote back address " + address + " (" + ByteBuffer.wrap(data).getInt() + ")");
	}
	
	private boolean ProcessNodes() {
		boolean ret = false;
		for(Node n : this.nodes) {
			if(n.Process()) {
				//There is still a node who has stuff to process
				ret = true;
			}
		}
		return ret;
	}
	
	private void PassDataToOwner() {
		//All the acks are in for a nodes request, send them the data/acks
		if(this.busOwner == -1) {
			System.out.println("ERROR: In PassDataToOwner, the bus owner is not set, returning");
			return;
		}
		if(this.busReqAddr == -1) {
			System.out.println("ERROR: In PassDataToOwner, bus request address is not set, returning");
			return;
		}
		BusItem item;
		boolean dataGiven = this.dataForRequest != null;
		State blockState = GetBlockState(dataGiven);
		if(dataGiven) {
			//Data is there
			//Create the BusAcks item and pass to the bus owning node
			item = new BusAcks(this.busReqAddr.intValue(), this.dataForRequest.clone(), blockState);
		} else {
			//Data is not there, need to grab it from memory
			byte[] data = this.memory.getData(this.busReqAddr);
			item = new BusAcks(this.busReqAddr, data, blockState);
		}
		System.out.println("BC: Passed back acknowledgements for address " + this.busReqAddr.intValue() + " to node " + this.busOwner);
		this.qman.BC2RespPush(item, this.busOwner);
		ResetBusRequestValues();
	}
	
	private void TryGrantBus() {
		//See if anyone wants the bus, if they do grab the item from their Q and broadcast it
		BusItem bi = FindItem();
		if(bi != null) {
			ProcessRequest(bi);
		}
	}
	
	//Looks for a request from next node in line
	private BusItem FindItem() {
		//Need to fairly grant bus ownership
		BusItem bi = null;
		int curr;
		if(this.prevBusOwner == -1) {
			curr = 0;
		} else {
			curr = (this.prevBusOwner + 1) % this.numberOfNodes;
		}
		for(int i = 0; i < this.numberOfNodes; i++) {
			bi = qman.Requ2BCPull(curr);
			if(bi != null) {
				//We have a request
				if(bi instanceof BusRequest) {
					this.busOwner = curr;
					break;
				} else {
					System.out.println("ERROR: In FindItem, item pulled from Requ2BC for node " + i + "was not of type BusRequest, returning null");
					return null;
				}
			}
			curr = (curr + 1) % this.numberOfNodes;
		}
		return bi;
	}
	
	private void TryGrabAcks() {
		//Loop through each node and check QManager.Resp2BC (which will return an ack if any exist)
		BusItem item = null;
		for(int i = 0; i < this.numberOfNodes; i++) {
			if(i == this.busOwner) continue;
			item = this.qman.Resp2BCPull(i);
			if(item != null) {
				if(!(item instanceof BusAck)) {
					System.out.println("ERROR: Inside TryGrabAcks, item pulled from node " + i + " Resp2BC was not of type BusAcks, returning");
					return;
				}
				//We have an ack
				ProcessAck((BusAck) item);
				break;
			}
		}
	}
	
	private void ProcessRequest(BusItem item) {
		this.busTransCount++;
		//Set other BC vars to the new request we have
		BusRequest br = (BusRequest) item;
		this.busReqAddr = br.getAddress();
		this.typeOfBusRequest = br.getType();

		//Broadcast a request for a node to all other nodes
		for(int i = 0; i < this.numberOfNodes; i++) {
			//Send out request to all nodes besides the one who sent the request (aka busOwner)
			if(i == this.busOwner) continue;
			//Put this request into the response controller for each node
			this.qman.BC2RespPush(item, i);
		}
		if(this.numberOfNodes == 1) {
			//Just say the acks are in and it will grab the data next cycle
			this.acksReady = true;
		}
	}
	
	private void ResetBusRequestValues() {
		this.responseNum = 0;
		this.busReqAddr = -1;
		this.dataForRequest = null;
		this.prevBusOwner = this.busOwner;
		this.busOwner = -1;
		this.acksReady = false;
		this.typeOfBusRequest = null;
	}
	
	private void ProcessAck(BusAck item) {
		//Grab variables
		byte[] dataFromNode = item.getData();
		Integer address = item.getAddress();
		if(!address.equals(this.busReqAddr)) {
			System.out.println("ERROR: In ProcessAck, node " + item.getNodeNum() + " returned BusAck with address " + 
								address + ", but the BC was waiting for BusAck for address " + this.busReqAddr);
			return;
		}
		
		this.responseNum++;

		//Grab any data that the node sent back = if node sent data back it needs to be written to memory
		if(dataFromNode != null) {
			this.memory.setData(address, dataFromNode.clone());
			if(this.dataForRequest != null)
				System.out.println("WARNING: In ProcessAck, a node sent back data with an ack but the BC dataForRequest has already been set");
			this.dataForRequest = dataFromNode.clone();
		}

		//if all acks are in, Set acksReady = true
		if(this.responseNum == this.numberOfNodes - 1) {
			//All acks are in
			this.acksReady = true;
		} else if(this.responseNum > this.numberOfNodes - 1) {
			System.out.println("ERROR: In ProcessAck, the number of responses received > number of nodes who need to send a reponse, returning");
			return;
		}
	}
	
	private State GetBlockState(boolean dataGiven) {
		//If BusRead:
			//if we grabbed it from memory
				//exclusive
			//if another node sent us it
				//shared
		//If BusReadEx/BusUpgrade:
			//modified, so that L1C knows it can write to it
		switch(this.typeOfBusRequest) {
			case BUSREAD:
				if(dataGiven) {
					//Another node sent us the data
					return State.SHARED;
				} else {
					//We will grab the data from memory
					return State.EXCLUSIVE;
				}
			case BUSREADEX:
				return State.MODIFIED;
			case BUSUPGRADE:
				return State.MODIFIED;
			default:
				System.out.println("ERROR: In BC.GetBlockState, type of BusRequest not caught, returning null");
				return null;
		}
	}
}
