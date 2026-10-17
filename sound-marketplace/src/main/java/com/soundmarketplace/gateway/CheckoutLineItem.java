package com.soundmarketplace.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutLineItem {
    private String name;
    private long unitAmountCents;
    private String currency;
    private long quantity;
}
