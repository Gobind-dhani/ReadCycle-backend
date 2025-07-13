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

    public void sendBuyOrderConfirmation(String mobile, String customerName, String awb, String items, String amountPaid, String trackingUrl) {
        String formattedMobile = formatMobile(mobile);
        String url = "https://api.gupshup.io/wa/api/v1/msg";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("apikey", gupshupApiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("channel", "whatsapp");
        body.add("source", "15557653121"); // your WABA number
        body.add("src.name", "Readcycle");
        body.add("destination", formattedMobile);

        body.add("type", "template");
        body.add("template.id", "1e6f2b37-7ce9-40dd-a3cc-2478373b7873");

        // Add template params for placeholders {{1}} to {{5}}
        body.add("template.params[0]", customerName);
        body.add("template.params[1]", awb);
        body.add("template.params[2]", items);
        body.add("template.params[3]", amountPaid);
        body.add("template.params[4]", trackingUrl);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            System.err.println("❌ Failed to send WhatsApp order confirmation: " + e.getMessage());
        }
    }


    private String formatMobile(String mobile) {
        mobile = mobile.replaceAll("[^\\d]", "");
        if (mobile.startsWith("0")) {
            mobile = mobile.substring(1);
        }
        if (!mobile.startsWith("91")) {
            mobile = "91" + mobile;
        }
        return mobile;
    }
}
