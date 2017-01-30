package general;

public class QItem {

	private Instruction instruction;
	private byte[] data;
	
	public QItem(Instruction i) {
		this.instruction = i;
	}
	
	public QItem(Instruction i, byte[] d) {
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

}
