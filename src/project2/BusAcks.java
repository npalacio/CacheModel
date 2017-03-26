package project2;

public class BusAcks extends BusItem {

	//This is an item that the BC would send to the Response Controller of a node
	//when all the acks were in for that nodes request
	
	private byte[] data;
	
	public BusAcks(int address, byte[] data) {
		super(address);
		this.data = data;
	}
}
