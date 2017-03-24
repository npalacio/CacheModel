package project2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import general.Instruction;
import general.Read;
import general.Write;

public class BusController {

	private QManager qman;
	private List<Node> nodes = new ArrayList<Node>();
	private Integer numberOfNodes = -1;
	private List<List<Instruction>> nodeInstructions;
	//Maps a node to the q's between that node and this BC
	private Map<Node, List<Queue<BusItem>>> nodesToQs = new HashMap<Node, List<Queue<BusItem>>>();
	private Integer busOwner = -1;
	private Integer prevBusOwner = -1;
	private Integer responseNum = 0;
	private boolean acksReady = false;
	private byte[] dataForRequest;
	private Integer cycleCount = 0;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BusController bc = new BusController();
		bc.Initialize(args);
	}
	
	private void Initialize(String[] args) {
		String fileName = args[0];
		//-1 implies no one owns the bus
		InitializeNodes();
		ProcessInstructions(fileName);
	}
	
	private void ProcessInstructions(String fileName) {
		FileInputStream fis = null;
		BufferedReader reader = null;
		try {
			fis = new FileInputStream(fileName);
			reader = new BufferedReader(new InputStreamReader(fis));
			String line = reader.readLine();
			int i = 0;
			while(line != null) {
				if(i == 0) {
					//This should just tell us the number of nodes
					ProcessFirstLine(line);
				} else if(line == ";") {
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
						instr = new Read(i, Integer.parseInt(tokens[2]), Integer.parseInt(tokens[2]), node);
					} else if(tokens[1].equals("write") && tokens.length == 4) {
						node = Integer.parseInt(tokens[0]);
						instr = new Write(i, Integer.parseInt(tokens[2]), ByteBuffer.allocate(32).putInt(Integer.parseInt(tokens[3])).array(), node);
					} else {
						System.out.println("Invalid input for instruction");
					}
					//Add the instruction to the corresponding node
					nodeInstructions.get(node).add(instr);
				}
				i++;
				line = reader.readLine();
			}
			//TODO: Loop through process until we are done (FinishOutProcessing()?)
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				reader.close();
			} catch(IOException e) {
				System.out.println(e);
			}
		}
	}

	private void ProcessFirstLine(String line) {
		this.numberOfNodes = Integer.parseInt(line);
		if(this.numberOfNodes > 3) {
			System.out.println("WARNING: More than 3 nodes passed in (" + this.numberOfNodes + ")");
		}
		InitializeNodes();
	}
	
	private void SendInstructionBatch() {
		//TODO: Implement
		//Call process
		Process();
	}
	
	private void InitializeNodes() {
		//TODO: Implement
		this.nodeInstructions = new ArrayList<List<Instruction>>(this.numberOfNodes);
		this.qman = new QManager(this.numberOfNodes);
		for(int i = 0; i < this.numberOfNodes; i++) {
			CreateNode(i);
		}
	}

	private void CreateNode(int i) {
		Node node = new Node(i, qman);
		this.nodes.add(i, node);
	}
	
	private boolean Process() {
		//TODO: Implement
		//Every cycle we can only do 1 of these things:
			//Broadcast a request for a node
			//Send data to a node (if all acks are in)
			//grab an acknowledgement for a node
		//Check if anyone needs the bus (if no one currently owns it) and grant them the bus
		//If someone got the bus, broadcast their request if necessary
		boolean notDone = false;
		if(acksReady) {
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
		//Call QManager.Resp2BCPull(nodeNum) on all nodes to grab any acknowledgements available
		if(this.busOwner != -1) {
			TryGrabAcks();
		}
		this.cycleCount++;
		//If any nodes are still processing we are not done
		//If there is a bus owner still we are not done (waiting on acks)
		boolean ret = notDone || (this.busOwner != -1);
		return ret;
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
		//TODO: Implement
		//All the acks are in for a nodes request, send them the data/acks
	}
	
	private void TryGrantBus() {
		//TODO: Implement
		//See if anyone wants the bus, if they do grab the item from their Q and broadcast it
		BusItem bi = FindItem();
		if(bi != null) {
			Broadcast(bi);
		}
	}
	
	//Looks for a request from next node in line
	private BusItem FindItem() {
		//Need to fairly grant bus ownership
		BusItem bi = null;
		int start;
		if(this.prevBusOwner == -1) {
			start = 0;
		} else {
			start = (this.prevBusOwner + 1) % 3;
		}
		for(int i = 0; i < this.numberOfNodes; i++) {
			bi = qman.Requ2BCPull(start);
			if(bi != null) {
				//We have a request
				break;
			}
			start = (start + 1) % 3;
		}
		return bi;
	}
	
	private void TryGrabAcks() {
		//TODO: Implement
		//Loop through each node and check QManager.Resp2BC (which will return an ack if any exist)
			//BREAK AFTER GRABBING ONE ACK
			//Grab any data that the node sent back
			//Increment this.responseNum
		//if all acks are in and you take away bus ownership be sure to keep track of prevBusOwner and reset responseNum
		//and set acksReady = true
		//if it needed data be sure to give it data, grab from memory if necessary
	}
	
	private void Broadcast(BusItem item) {
		//TODO: Implement
	}
}
