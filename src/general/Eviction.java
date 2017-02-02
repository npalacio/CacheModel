package general;

public class Eviction extends Instruction {

	private byte[] data;
	
	//Boolean so that when an eviction comes from L1D it can tell L1C if it should go to WB or Victim cache
	private boolean dirty;
	//Boolean so that L1D knows if this eviction address can just be moved to WB/Victim or if it has to leave 
	//L1 entirely to maintain mutual inclusion
	private boolean fromL2ToL1;

	public Eviction(int addr, byte[] d) {
		super(addr);
		this.data = d;
	}

	public Eviction(int addr) {
		super(addr);
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

	public boolean isFromL2ToL1() {
		return fromL2ToL1;
	}

	public void setFromL2ToL1(boolean fromL2ToL1) {
		this.fromL2ToL1 = fromL2ToL1;
	}
	
}
