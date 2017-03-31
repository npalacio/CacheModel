package project2;

public class BusWriteBack extends BusItem {

	//This is an item that L1C will send to the request controller to write an address back to
	//memory that L1C had to evict
	
	private byte[] data;
	private Integer nodeNum;
	
	public BusWriteBack(Integer address, byte[] data, Integer nodeNum) {
		super(address);
		this.data = data;
		this.nodeNum = nodeNum;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Integer getNodeNum() {
		return nodeNum;
	}

	public void setNodeNum(Integer nodeNum) {
		this.nodeNum = nodeNum;
	}
	
	
}
