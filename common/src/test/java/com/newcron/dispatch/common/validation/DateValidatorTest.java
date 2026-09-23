package com.newcron.dispatch.common.validation;

import com.newcron.dispatch.common.model.OrderResult;
import com.newcron.dispatch.common.model.ShipperOrderRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);

    private ShipperOrderRequest requestWith(String pickup, String delivery) {
        ShipperOrderRequest request = new ShipperOrderRequest();
        request.setShipperOrderId("TEST-1");
        request.setPickupDate(pickup);
        request.setDeliveryDate(delivery);
        return request;
    }

    @Test
    void validRequestIsAccepted() {
        ShipperOrderRequest request = requestWith("2026-09-24", "2026-09-26");
        assertTrue(DateValidator.validate(request, TODAY, LocalTime.of(10, 0)).isEmpty());
    }

    @Test
    void pastPickupDateIsCancelled() {
        ShipperOrderRequest request = requestWith("2026-09-20", "2026-09-25");
        Optional<OrderResult> result = DateValidator.validate(request, TODAY, LocalTime.of(10, 0));
        assertTrue(result.isPresent());
        assertEquals("Cancelled", result.get().getStatus());
    }

    @Test
    void sameDayPickupBeforeCutoffIsAccepted() {
        ShipperOrderRequest request = requestWith("2026-09-23", "2026-09-25");
        assertTrue(DateValidator.validate(request, TODAY, LocalTime.of(14, 0)).isEmpty());
    }

    @Test
    void sameDayPickupAfterCutoffIsCancelled() {
        ShipperOrderRequest request = requestWith("2026-09-23", "2026-09-25");
        assertTrue(DateValidator.validate(request, TODAY, LocalTime.of(15, 30)).isPresent());
    }

    @Test
    void deliverySameDayAsPickupIsCancelled() {
        ShipperOrderRequest request = requestWith("2026-09-24", "2026-09-24");
        assertTrue(DateValidator.validate(request, TODAY, LocalTime.of(9, 0)).isPresent());
    }
}
