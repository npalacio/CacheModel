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
			entry = new CacheEntry(i, new byte[32]);
//			entry = new CacheEntry(i, ByteBuffer.allocate(32).putInt(i).array());
			data.add(i, entry);
		}
	}
	
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
