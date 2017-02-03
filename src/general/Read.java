package general;

public class Read extends Instruction {

	private int byteOffset;
	
	public Read(int num, int addr) {
		super(addr, num);
		this.byteOffset = 0;
	}
	
	public Read(int num, int addr, int byteOff) {
		super(addr, num);
		this.byteOffset = byteOff;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public void setByteOffset(int byteOffset) {
		this.byteOffset = byteOffset;
	}


	@Override
	public String toString() {
		return "Read from " + this.getAddress();
	}
}
