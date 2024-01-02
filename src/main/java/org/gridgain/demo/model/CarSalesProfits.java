package org.gridgain.demo.model;

import java.io.Serializable;
import java.sql.Date;
import java.util.UUID;

public class CarSalesProfits implements Serializable {

    private static final long serialVersionUID = 2532528408811237748L;

    private UUID id;

    private Date saleDate;

    private Double totalExpense;

    private Double totalIncome;

    private Boolean isFinal;

    public CarSalesProfits() {
        this(null, null, null, null, null);
    }

    public CarSalesProfits(UUID id, Date saleDate, Double totalExpense, Double totalIncome, Boolean isFinal) {
        this.id = id;
        this.saleDate = saleDate;
        this.totalExpense = totalExpense;
        this.totalIncome = totalIncome;
        this.isFinal = isFinal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Date getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(Date saleDate) {
        this.saleDate = saleDate;
    }

    public Double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(Double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Boolean getIsFinal() { return isFinal; }

    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }

}
