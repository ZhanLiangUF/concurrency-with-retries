package Models;

public class AddressGeoCode {
	private String address;
	private String status;
	private Location location;
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public AddressGeoCode(String address, String status, Location location) {
		this.address = address;
		this.status = status;
		this.location = location;
	}
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
}
