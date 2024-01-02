package org.gridgain.demo.model;

import java.io.Serializable;
import java.sql.Date;

public class CarSales implements Serializable {

    private static final long serialVersionUID = 4927076757625397794L;

    private String vin;

    private Date saleDate;

    private Double purchaseCost;

    private Double salePrice;

    public CarSales() {
        this(null, null, null, null);
    }

    public CarSales(String vin, Date saleDate, Double purchaseCost, Double salePrice) {
        this.vin = vin;
        this.saleDate = saleDate;
        this.purchaseCost = purchaseCost;
        this.salePrice = salePrice;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public Date getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(Date saleDate) {
        this.saleDate = saleDate;
    }

    public Double getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(Double purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public Double getPrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

}
