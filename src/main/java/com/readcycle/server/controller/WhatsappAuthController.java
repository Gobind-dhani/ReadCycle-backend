package com.readcycle.server.controller;

import com.readcycle.server.entity.User;
import com.readcycle.server.repository.UserRepository;
import com.readcycle.server.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;import java.util.*;


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
        Map<String, Object> response = new HashMap<>();

        // Generate OTP
        String otp = String.valueOf(new Random().nextInt(899999) + 100000);
        otpStore.put(mobile, otp);

        String url = "https://api.gupshup.io/wa/api/v1/template/msg";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add("apikey", gupshupApiKey);  // Your Gupshup API Key

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("channel", "whatsapp");
            requestBody.add("source", "15557653121"); // your WABA number
            requestBody.add("src.name", "Readcycle"); // your app name as per WABA
            requestBody.add("destination", "91" + mobile);

            // Template message
            String name = "there";// this must match the exact ID from Gupshup
            String templateJson = String.format("{\"id\":\"888d953e-755f-46e2-8955-c195029bb976\",\"params\":[\"%s\"]}", name);

            requestBody.add("template", templateJson);

            // Optional: Add postback text if needed
//            requestBody.add("postbackTexts", "[{\"index\":0,\"text\":\"Verify\"}]");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(url, entity, String.class);

            return ResponseEntity.status(apiResponse.getStatusCode()).body(apiResponse.getBody());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP via Gupshup", e);
        }
    }



    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestParam String mobile, @RequestParam String otp, HttpServletResponse res) {
        Map<String, Object> response = new HashMap<>();

        String storedOtp = otpStore.get(mobile);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            response.put("error", "Invalid or expired OTP");
            return response;
        }

        otpStore.remove(mobile); // Remove used OTP

        // Check if user exists
        Optional<User> userOpt = userRepository.findByPhone(mobile);
        User user = userOpt.orElseGet(() -> {
            User newUser = new User();
            newUser.setPhone(mobile);
            newUser.setProvider("whatsapp");
            newUser.setProviderId(mobile);
            newUser.setName("User" + mobile.substring(6));
            newUser.setEmail(null);
            newUser.setAvatarUrl(null);
            return userRepository.save(newUser);
        });

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail());


        response.put("token", token);
        response.put("user", user);
        return response;
    }
}
