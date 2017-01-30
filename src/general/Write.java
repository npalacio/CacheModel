package general;

public class Write extends Instruction {

	private byte[] data;
	
	public Write(int addr, byte[] d) {
		super(addr);
		this.data = d;
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
