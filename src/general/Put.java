package general;

public class Put extends Instruction {

	private byte[] data;
	private boolean isEntryDirty;

	//This instruction is for L1C --> L1D and L2C --> L2D 
	//so that the data's can know they need to kick something out to make room for new data coming in 
	public Put(int addr, byte[] d) {
		super(addr);
		this.data = d.clone();
	}
	
	public boolean isEntryDirty() {
		return isEntryDirty;
	}

	public void setEntryDirty(boolean isEntryDirty) {
		this.isEntryDirty = isEntryDirty;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
