package general;

public class ControllerEntry {
	private boolean valid;
	private boolean dirty;
	private int address;
	
	public ControllerEntry(boolean v, boolean d, int addr) {
		this.valid = v;
		this.dirty = d;
		this.address = addr;
	}
	
	@Override
	public String toString() {
		String s = "Address: " + this.address + ", valid: " + this.valid + ", dirty: " + this.dirty;
		return s;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}
}
