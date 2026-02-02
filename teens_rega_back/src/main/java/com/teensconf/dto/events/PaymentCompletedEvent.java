package com.teensconf.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long registrationId;
    private Long receiptId;
    private boolean paid;
    private boolean verified;
}
