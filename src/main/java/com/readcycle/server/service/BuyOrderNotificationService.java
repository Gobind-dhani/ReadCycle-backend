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

    public void sendBuyOrderConfirmation(String mobile, String awb) {
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

        String customMessage = String.format(
                "🎉 Thank you for your order on Readcycle!\n\nYour books are on the way 📦\nAWB No: %s\nTrack anytime from your profile section.\n\nHappy Reading! 📚",
                awb
        );

        body.add("message", customMessage);

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
