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
public class SellOrderNotificationService {

    @Value("${gupshup.api.key}")
    private String gupshupApiKey;

    @Value("${gupshup.whatsapp.sellorder.template}")
    private String gupshupSellOrderTemplate;

    public void sendSellOrderConfirmation(String mobile, String customerName, String bookTitle, String pickupAddress, String paymentMethod, String amount) {
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

            // Build template JSON matching your new sell order template
            String templateJson = String.format(
                    "{\"id\":\"%s\",\"params\":[\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"]}",
                    gupshupSellOrderTemplate, customerName, bookTitle, pickupAddress, paymentMethod, amount
            );
            body.add("template", templateJson);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> apiResponse = restTemplate.postForEntity(url, entity, String.class);

            System.out.println("✅ WhatsApp sell order confirmation sent. Response: " + apiResponse.getBody());

        } catch (Exception e) {
            System.err.println("❌ Failed to send WhatsApp sell order confirmation: " + e.getMessage());
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
