package general;

public abstract class Instruction {

	private int address;
	
	public Instruction(int addr){
		this.address = addr;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}
}
