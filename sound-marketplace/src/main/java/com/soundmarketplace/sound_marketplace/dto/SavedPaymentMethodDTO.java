package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of a customer's saved payment method (e.g. Visa ending in 4242)
 * securely stored in Stripe's PCI-compliant vault.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPaymentMethodDTO {
    private String id;
    private String brand;
    private String last4;
    private Long expMonth;
    private Long expYear;
    private Boolean isDefault;
}
