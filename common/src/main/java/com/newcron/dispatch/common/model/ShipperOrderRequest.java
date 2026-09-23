package com.newcron.dispatch.common.model;

import java.util.List;

/**
 * Representa el payload que un cliente (dueño de un predio de autos) envía a NewCron
 * solicitando el transporte de uno o más vehículos.
 */
public class ShipperOrderRequest {

    private String shipperOrderId;
    private String pickupDate;
    private String deliveryDate;
    private double price;
    private List<Stop> stops;
    private List<Vehicle> vehicles;
    private String transportationReleaseNotes;

    public ShipperOrderRequest() {
    }

    public String getShipperOrderId() {
        return shipperOrderId;
    }

    public void setShipperOrderId(String shipperOrderId) {
        this.shipperOrderId = shipperOrderId;
    }

    public String getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(String pickupDate) {
        this.pickupDate = pickupDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public void setStops(List<Stop> stops) {
        this.stops = stops;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public String getTransportationReleaseNotes() {
        return transportationReleaseNotes;
    }

    public void setTransportationReleaseNotes(String transportationReleaseNotes) {
        this.transportationReleaseNotes = transportationReleaseNotes;
    }
}
