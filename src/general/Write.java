package general;

public class Write extends Instruction {

	private byte[] data;
	
	public Write(int num, int addr, byte[] d, int nodeNum) {
		super(addr, num, nodeNum);
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
		return "(" + this.instrNum + ") Write " + java.nio.ByteBuffer.wrap(this.data).getInt() + " to " + this.getAddress();
	}
}
