package com.readcycle.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class BuyOrderNotificationService {

    @Value("${gupshup.api.key}")
    private String gupshupApiKey;

    @Value("${gupshup.whatsapp.order.template}")
    private String gupshupOrderTemplate;

    public void sendBuyOrderConfirmation(String mobile, String customerName, String awb, String items, String amountPaid, String trackingUrl) {
        String formattedMobile = formatMobile(mobile);
        String url = "https://api.gupshup.io/wa/api/v1/template/msg";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add("apikey", gupshupApiKey);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("channel", "whatsapp");
            body.add("source", "15557653121"); // your WABA number
            body.add("src.name", "Readcycle");
            body.add("destination", formattedMobile);

            // Build template JSON matching your OTP sending format
            String templateJson = String.format(
                    "{\"id\":\"%s\",\"params\":[\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"]}",
                    gupshupOrderTemplate, customerName, awb, items, amountPaid, trackingUrl
            );
            body.add("template", templateJson);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> apiResponse = restTemplate.postForEntity(url, entity, String.class);

            System.out.println("✅ WhatsApp order confirmation sent. Response: " + apiResponse.getBody());

        } catch (Exception e) {
            System.err.println("❌ Failed to send WhatsApp order confirmation: " + e.getMessage());
        }
    }

    private String formatMobile(String mobile) {
        mobile = mobile.replaceAll("[^\\d]", ""); // Keep only digits
        if (mobile.startsWith("0")) {
            mobile = mobile.substring(1);
        }
        if (!mobile.startsWith("91")) {
            mobile = "91" + mobile;
        }
        return mobile;
    }
}
