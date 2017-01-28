package general;

public class Read extends Instruction {

	public Read(int addr) {
		super(addr);
	}
	
	@Override
	public String toString() {
		return "Read from " + this.getAddress();
	}
}
