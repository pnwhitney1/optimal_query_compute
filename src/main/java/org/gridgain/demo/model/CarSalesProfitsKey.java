package org.gridgain.demo.model;

import java.io.Serializable;
import java.sql.Date;
import java.util.Objects;
import java.util.UUID;

public class CarSalesProfitsKey implements Serializable {

    private static final long serialVersionUID = -636539849953838231L;

    private UUID id;

    private Date saleDate;

    public CarSalesProfitsKey(UUID id, Date saleDate) {
        this.id = id;
        this.saleDate = saleDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CarSalesProfitsKey key = (CarSalesProfitsKey)o;

        if (this.id != key.id)
            return false;
        return this.saleDate == key.saleDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, saleDate);
    }

}
