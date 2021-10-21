package com.digitald4.common.model;

public class Company {
    private String name;
    private String website;
    private String slogan;
    private String description;
    private String ipAddress;
    private String email;
    private String paypal;
    private String statCounterId;
    private String statCounterPart;
    private String container;
    private String address;
    private String phone;
    private String fax;
    private String reportFooter;

    public String getName() {
        return name;
    }

    public Company setName(String name) {
        this.name = name;
        return this;
    }

    public String getWebsite() {
        return website;
    }

    public String getSlogan() {
        return slogan;
    }

    public String getDescription() {
        return description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getEmail() {
        return email;
    }

    public String getPaypal() {
        return paypal;
    }

    public String getStatCounterId() {
        return statCounterId;
    }

    public String getStatCounterPart() {
        return statCounterPart;
    }

    public String getContainer() {
        return container;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getFax() {
        return fax;
    }

    public String getReportFooter() {
        return reportFooter;
    }
}
