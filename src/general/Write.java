package general;

public class Write extends Instruction {

	private int number;
	private byte[] data;
	
	public Write(int num, int addr, byte[] d) {
		super(addr);
		this.number = num;
		this.data = d;
	}
	
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Write " + java.nio.ByteBuffer.wrap(this.data).getInt() + " to " + this.getAddress();
	}
}
