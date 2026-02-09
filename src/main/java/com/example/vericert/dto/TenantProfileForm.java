package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TenantProfileForm {

    @NotBlank(message = "Ragione sociale obbligatoria")
    @Size(max = 160)
    private String companyName;

    @NotBlank(message = "P.IVA obbligatoria")
    @Size(max = 32)
    private String vatNumber;

    @Size(max = 32)
    private String taxCode;

    @Size(max = 16)
    private String sdiCode;

    @Email(message = "PEC non valida")
    @Size(max = 160)
    private String pecEmail;

    @Size(max = 160)
    private String addressLine1;

    @Size(max = 160)
    private String addressLine2;

    @Size(max = 80)
    private String city;

    @Size(max = 8)
    private String province;

    @Size(max = 16)
    private String postalCode;

    @Pattern(regexp = "IT", message = "Per ora vendi solo in Italia (IT)")
    private String country = "IT";

    @Email(message = "Email assistenza non valida")
    @Size(max = 160)
    private String supportEmail;

    @Size(max = 255)
    private String websiteUrl;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getSdiCode() {
        return sdiCode;
    }

    public void setSdiCode(String sdiCode) {
        this.sdiCode = sdiCode;
    }

    public String getPecEmail() {
        return pecEmail;
    }

    public void setPecEmail(String pecEmail) {
        this.pecEmail = pecEmail;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }
}
