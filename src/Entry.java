package current;
import java.util.Date;

/*
 * Name: Michael Frake
 * Date: Oct 14, 2021
 * Description: 
 */

public class Entry {
	private Date effectiveDate = new Date();
	private String vendor = "";
	private double amount = 0.0;
	private String[] data;
	public Entry(Date date, String vendor, double amount) {
		this.effectiveDate = date;
		this.vendor = vendor;
		this.amount = amount;
		data = new String[] {effectiveDate.toString(), vendor, "Cost of good sold", "CC", Double.toString(amount)};
	}
	
	@Override
	public String toString() {
		return data.toString();
	}
	
	public String[] getStringData() {
		return data;
	}

	public String getVendor() {
		return vendor;
	}

	public double getAmount() {
		return amount;
	}
}
