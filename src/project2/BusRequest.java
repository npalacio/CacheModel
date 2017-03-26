package project2;

public class BusRequest extends BusItem {
	//RequestController --> BC
	//Requests for data/upgrades
	private RequestType type;
	
	public BusRequest(Integer addr, RequestType type) {
		super(addr);
		this.type = type;
	}
	
}
