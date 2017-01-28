package level1;

public class L1DEntry {
	
	private int address;
	private byte[] data;
	
	public L1DEntry(int addr, byte[] d) {
		this.address = addr;
		this.data = d;
	}
	
	@Override
	public String toString() {
		return null;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
