package general;

public class QItem {

	private Instruction instruction;
	private byte[] data;
	//L2 could be sending us an entry that is dirty (Got sent to L2 but never written back to memory)
	//So if L1 asks for this data again, it needs to know if it is dirty or clean
	private boolean isDataDirty;
	
	public QItem(Instruction i) {
		this.instruction = i;
	}
	
	public QItem(Instruction i, byte[] d) {
		this.instruction = i;
		this.data = d;
	}

	public Instruction getInstruction() {
		return instruction;
	}

	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean isDataDirty() {
		return isDataDirty;
	}

	public void setDataDirty(boolean isDataDirty) {
		this.isDataDirty = isDataDirty;
	}

}
