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
}
