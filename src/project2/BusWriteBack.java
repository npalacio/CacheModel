package project2;

public class BusWriteBack extends BusItem {

	//This is an item that L1C will send to the request controller to write an address back to
	//memory that L1C had to evict
	
	private byte[] data;
	
	public BusWriteBack(Integer address, byte[] data) {
		super(address);
		this.data = data;
	}
}
