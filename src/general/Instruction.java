package general;

public abstract class Instruction {

	private int node;
	protected int instrNum;
	private int address;
	
	public Instruction(int addr, int nodeNum){
		this.address = addr;
	}

	public Instruction(int addr, int num, int nodeNum){
		this.address = addr;
		this.instrNum = num;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}
	
	public int getNumber() {
		return instrNum;
	}

	public void setNumber(int number) {
		this.instrNum = number;
	}

	public int getNode() {
		return node;
	}

	public void setNode(int node) {
		this.node = node;
	}
	
}
