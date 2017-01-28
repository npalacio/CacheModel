package general;

public class Eviction extends Instruction {

	private byte[] data;
	
	public Eviction(int addr, byte[] d) {
		super(addr);
		this.data = d;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
