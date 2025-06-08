package com.readcycle.server.controller;

import com.readcycle.server.entity.User;
import com.readcycle.server.repository.UserRepository;
import com.readcycle.server.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class WhatsappAuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private final Map<String, String> otpStore = new HashMap<>(); // mobile -> otp

    @Value("${gupshup.api.key}")
    private String gupshupApiKey;

    @Value("${gupshup.whatsapp.template}")
    private String gupshupTemplate;

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String mobile) {
        String formattedMobile = formatMobile(mobile);

        // Generate OTP
        String otp = String.valueOf(new Random().nextInt(899999) + 100000);
        otpStore.put(formattedMobile, otp);

        String url = "https://api.gupshup.io/wa/api/v1/template/msg";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add("apikey", gupshupApiKey);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("channel", "whatsapp");
            requestBody.add("source", "15557653121"); // your WABA number
            requestBody.add("src.name", "Readcycle");
            requestBody.add("destination", formattedMobile);

            String templateJson = String.format(
                    "{\"id\":\"%s\",\"params\":[\"%s\"]}",
                    gupshupTemplate, otp
            );
            requestBody.add("template", templateJson);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(url, entity, String.class);

            return ResponseEntity.status(apiResponse.getStatusCode()).body(apiResponse.getBody());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP via Gupshup", e);
        }
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> payload, HttpServletResponse res) {
        Map<String, Object> response = new HashMap<>();

        String mobile = payload.get("phone");
        String otp = payload.get("otp");

        if (mobile == null || otp == null) {
            response.put("error", "Missing phone or OTP");
            return response;
        }

        String formattedMobile = formatMobile(mobile);
        String storedOtp = otpStore.get(formattedMobile);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            response.put("error", "Invalid or expired OTP");
            return response;
        }

        otpStore.remove(formattedMobile);

        // Check if user exists by provider + providerId
        Optional<User> userOpt = userRepository.findByProviderAndProviderId("whatsapp", formattedMobile);
        User user = userOpt.orElseGet(() -> {
            String name = "User" + formattedMobile.substring(Math.max(0, formattedMobile.length() - 4));
            User newUser = new User(
                    formattedMobile,     // providerId
                    "whatsapp",          // provider
                    name,                // name
                    null,                // email
                    null,                // avatarUrl
                    formattedMobile      // phone
            );
            return userRepository.save(newUser);
        });

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail());

        response.put("token", token);
        response.put("user", user);
        return response;
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
