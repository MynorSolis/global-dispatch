package com.newcron.dispatch.common.validation;

import com.newcron.dispatch.common.model.OrderResult;
import com.newcron.dispatch.common.model.ShipperOrderRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Aplica las reglas de negocio descritas en el documento del reto antes de
 * encolar un pedido:
 *
 *  1) pickupDate no puede ser anterior a la fecha actual.
 *  2) Si pickupDate es igual a hoy, la solicitud no puede llegar después de las 3:00 p.m.
 *  3) deliveryDate debe ser mayor a pickupDate, con al menos un día de diferencia.
 *
 * Si alguna regla falla, se retorna el OrderResult "Cancelled" correspondiente.
 * Si todas pasan, se retorna Optional.empty() (el pedido es válido).
 */
public final class DateValidator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final LocalTime SAME_DAY_CUTOFF = LocalTime.of(15, 0);

    private DateValidator() {
    }

    /** Valida usando la fecha/hora reales del sistema. */
    public static Optional<OrderResult> validate(ShipperOrderRequest request) {
        return validate(request, LocalDate.now(), LocalTime.now());
    }

    /** Sobrecarga que recibe "hoy" y "ahora" explícitos, útil para pruebas unitarias. */
    public static Optional<OrderResult> validate(ShipperOrderRequest request, LocalDate today, LocalTime now) {
        LocalDate pickup = LocalDate.parse(request.getPickupDate(), DATE_FORMAT);
        LocalDate delivery = LocalDate.parse(request.getDeliveryDate(), DATE_FORMAT);

        if (pickup.isBefore(today)) {
            return Optional.of(new OrderResult(
                    request.getShipperOrderId(),
                    "Cancelled",
                    "Pickup date cannot be earlier than the current date."
            ));
        }

        if (pickup.isEqual(today) && now.isAfter(SAME_DAY_CUTOFF)) {
            return Optional.of(new OrderResult(
                    request.getShipperOrderId(),
                    "Cancelled",
                    "Same-day pickup requests cannot be accepted after 3:00 p.m."
            ));
        }

        long daysBetween = ChronoUnit.DAYS.between(pickup, delivery);
        if (daysBetween < 1) {
            return Optional.of(new OrderResult(
                    request.getShipperOrderId(),
                    "Cancelled",
                    "Delivery date must be at least one day after the pickup date."
            ));
        }

        return Optional.empty();
    }
}
