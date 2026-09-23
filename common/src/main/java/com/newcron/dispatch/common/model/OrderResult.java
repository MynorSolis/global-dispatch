package com.newcron.dispatch.common.model;

/**
 * Payload de resultado que el cliente recibe en la cola de resultados,
 * indicando si su solicitud fue aceptada o cancelada.
 */
public class OrderResult {

    private String shipperOrderId;
    private String status;
    private String notes;

    public OrderResult() {
    }

    public OrderResult(String shipperOrderId, String status, String notes) {
        this.shipperOrderId = shipperOrderId;
        this.status = status;
        this.notes = notes;
    }

    public String getShipperOrderId() {
        return shipperOrderId;
    }

    public void setShipperOrderId(String shipperOrderId) {
        this.shipperOrderId = shipperOrderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
