package general;

import project2.State;

public class ControllerEntry {
	
	private State state;
	private int address;
	private byte[] data;
	
	public ControllerEntry(State s, int addr, byte[] data) {
		this.state = s;
		this.address = addr;
		this.data = data;
	}
	
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	@Override
	public String toString() {
		String s = "Address: " + this.address + ", state: " + this.state;
		return s;
	}

	/*Getters and Setters*/
	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}
	
	public Location getLoc() {
		return loc;
	}

	public void setLoc(Location loc) {
		this.loc = loc;
	}
}
