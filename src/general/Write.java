package general;

public class Write extends Instruction {

	private int value;
	
	public Write(int addr, int val) {
		super(addr);
		this.value = val;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Write " + this.getValue() + " to " + this.getAddress();
	}
}
