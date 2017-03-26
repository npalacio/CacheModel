package project2;

public class BusAcks extends BusItem {

	//This is an item that the BC would send to the Response Controller of a node
	//when all the acks were in for that nodes request
	
	private byte[] data;
	private State state;
	
	public BusAcks(int address, byte[] data, State state) {
		super(address);
		this.data = data;
		this.state = state;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
}
