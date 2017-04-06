package project2;

public abstract class BusItem {

	private Integer address;
	
	public BusItem(Integer address) {
		this.address = address;
	}

	public Integer getAddress() {
		return address;
	}

	public void setAddress(Integer address) {
		this.address = address;
	}
	
}
