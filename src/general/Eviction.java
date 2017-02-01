package general;

public class Eviction extends Instruction {

	private byte[] data;
	
	//Boolean so that when an eviction comes from L1D it can tell L1C if it should go to WB or Victim cache
	private boolean dirty;

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
	
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

}
