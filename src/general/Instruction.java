package general;

public abstract class Instruction {

	private int instrNum;
	private int address;
	
	public Instruction(int addr){
		this.address = addr;
	}

	public Instruction(int addr, int num){
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
}
