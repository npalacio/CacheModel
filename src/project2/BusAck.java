package project2;

public class BusAck extends BusItem {
	
	//This is an item that a node would send to the BC as an acknowledgement that it processes another nodes bus request
	
	private byte[] data = null;
	private Integer nodeNum;

	public BusAck(Integer nodeNum, Integer address) {
		super(address);
		this.nodeNum = nodeNum;
	}

	public BusAck(Integer nodeNum, Integer address, byte[] data) {
		super(address);
		this.nodeNum = nodeNum;
		this.data = data;
	}
	
	public byte[] getData() {
		return data.clone();
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
