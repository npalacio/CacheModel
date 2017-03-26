package general;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Memory {

	private List<CacheEntry> data;
	private int size;
	
	public Memory(int size) {
		this.size = size;
		initialize();
	}
	
	private void initialize() {
		data = new ArrayList<CacheEntry>(size);
		CacheEntry entry = null;
		for(int i = 0; i < size; i++) {
			entry = new CacheEntry(i, ByteBuffer.allocate(32).putInt(i).array());
			data.add(i, entry);
		}
	}
		
	/* Getters and Setters */
	public byte[] getData(int address) {
		byte[] ret = this.data.get(address).getData().clone();
		return ret;
		}
	
	public void setData(int address, byte[] data) {
		//Get the right CacheEntry
		CacheEntry e = this.data.get(address);
		//Set its data
		e.setData(data.clone());
	}

//	public List<CacheEntry> getData() {
//		return data;
//	}
//
//	public void setData(List<CacheEntry> data) {
//		this.data = data;
//	}

	public int getSize() {
		return size;
	}

//	public void setSize(int size) {
//		this.size = size;
//	}
}
