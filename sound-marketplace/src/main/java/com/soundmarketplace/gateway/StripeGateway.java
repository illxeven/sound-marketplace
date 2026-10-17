package com.soundmarketplace.gateway;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StripeGateway {

    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws Exception {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        String code = "200";
        try {
            return PaymentIntent.create(params);
        } catch (Exception e) {
            outcome = "error";
            code = e.getClass().getSimpleName();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=stripe op=paymentIntents.create durationMs={} outcome={} code={}", duration, outcome, code);
        }
    }
}
