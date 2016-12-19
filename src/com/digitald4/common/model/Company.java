package com.digitald4.common.model;

public class Company {
	private static Company company;
	public static Company get() {
		if(company == null) {
			company = new Company()
				.setName("IP360")
				.setWebsite("www.ip360app.com")
				.setSlogan("we are ip360")
				.setDescription("ip360 webapp")
				.setEmail("kenya@ip360.com")
				.setPaypal("")
				.setStatCounterID(null)
				.setStatCounterPart(null)
				.setContainer(null)
				.setAddress("7056 Archibald Ave, 102-375 Corona, Ca. 92880")
				.setPhone("Toll Free:  (877) 738-6135 Tel: 951-707-9989")
				.setFax("951-710-6699");
		}
		return company;
	}
	
	private String name="";
	private String website="";
	private String slogan="";
	private String description="";
	private String ipAddress="";
	private String email="";
	private String paypal="";
	private String statCounterID="";
	private String statCounterPart=""; 
	private String container="";
	private String address="";
	private String phone="";
	private String fax="";
	private String reportFooter;
	
	private Company() {
	}
	
	public Company setName(String name) {
		this.name = name;
		return this;
	}
	
	public String getName() {
		return name;
	}
	
	public Company setWebsite(String website) {
		this.website = website;
		return this;
	}
	
	public String getWebsite() {
		return website;
	}
	
	public String getSlogan() {
		return slogan;
	}
	
	public Company setSlogan(String slogan) {
		this.slogan = slogan;
		return this;
	}
	
	public Company setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Company setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		return this;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public Company setEmail(String email) {
		this.email = email;
		return this;
	}
	
	public String getEmail() {
		return email;
	}
	
	public Company setAddress(String address) {
		this.address = address;
		return this;
	}
	
	public String getAddress() {
		return address;
	}
	
	public Company setPhone(String phone) {
		this.phone = phone;
		return this;
	}
	
	public String getPhone() {
		return phone;
	}
	
	public Company setFax(String fax) {
		this.fax = fax;
		return this;
	}
	
	public String getFax() {
		return fax;
	}
	
	public Company setPaypal(String paypal) {
		this.paypal = paypal;
		return this;
	}
	
	public String getPaypal() {
		return paypal;
	}
	
	public Company setContainer(String container) {
		this.container = container;
		return this;
	}
	
	public String getContainer() {
		return container;
	}
	
	public Company setStatCounterID(String statCounterID) {
		this.statCounterID=statCounterID;
		return this;
	}
	
	public String getStatCounterID() {
		return statCounterID;
	}
	
	public Company setStatCounterPart(String statCounterPart) {
		this.statCounterPart=statCounterPart;
		return this;
	}
	
	public String getStatCounterPart() {
		return statCounterPart;
	}
	
	public Company setReportFooter(String reportFooter) {
		this.reportFooter = reportFooter;
		return this;
	}
	
	public String getReportFooter() {
		return reportFooter;
	}
}
