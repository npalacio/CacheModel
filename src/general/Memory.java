package general;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Memory {

	private List<CacheEntry> data;
	private int size = 131072;
	
	private Queue<QItem> toL2;
	private Queue<QItem> fromL2;
	
	public Memory(Queue<QItem> toL2, Queue<QItem> fromL2) {
		this.toL2 = toL2;
		this.fromL2 = fromL2;
		initialize();
	}
	
	private void initialize() {
		data = new ArrayList<CacheEntry>(size);
		CacheEntry entry = null;
		for(int i = 0; i < size; i++) {
//			entry = new CacheEntry(i, new byte[32]);
			entry = new CacheEntry(i, ByteBuffer.allocate(32).putInt(i).array());
			data.add(i, entry);
		}
	}
	
	public void process() {
		QItem q = this.fromL2.poll();
		if(q != null) {
			processFromL2(q);
		}
	}
	
	private void processFromL2(QItem q) {
		Instruction instr = q.getInstruction();
		int instrAddress = instr.getAddress();
		CacheEntry entry = this.data.get(instrAddress);
		if(instr instanceof Read || instr instanceof Write) {
			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", Memory: Fetching data from address " + instrAddress);
			byte[] dataNeeded = entry.getData().clone();
			q.setData(dataNeeded);
			this.toL2.offer(q);
			return;
		} else if(instr instanceof Eviction) {
			byte[] data = ((Eviction) instr).getData().clone();
			entry.setData(data);
			System.out.println("Instruction " + instr.getNumber() + ", " + instr.toString() + ", Memory: Writing to data from address " + instrAddress);
			return;
		} else {
			System.out.println("ERROR: L2C sent Memory an instruction other than a R/W, Eviction, stopping process!");
			return;
		}
	}
	
	/* Getters and Setters */
	public int getData(int address) {
		int ret = java.nio.ByteBuffer.wrap(this.data.get(address).getData()).getInt();
		return ret;
	}

	public List<CacheEntry> getData() {
		return data;
	}

	public void setData(List<CacheEntry> data) {
		this.data = data;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
