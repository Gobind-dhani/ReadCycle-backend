package com.readcycle.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcycle.server.entity.SellOrder;
import com.readcycle.server.repository.SellOrderRepository;
import com.readcycle.server.service.SellOrderNotificationService;
import com.readcycle.server.util.SSLUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/books/sell-orders")
@RequiredArgsConstructor
public class SellOrderController {

    private final SellOrderNotificationService whatsappService;
    private final SellOrderRepository sellOrderRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/create")
    public ResponseEntity<?> createSellOrder(@RequestBody Map<String, Object> requestData) {
        try {
            SSLUtil.disableSSLVerification();

            // Extract required fields from request
            String bookTitle = (String) requestData.get("bookTitle");
            Double expectedPrice = Double.valueOf(requestData.get("expectedPrice").toString());

            String name = (String) requestData.get("name");
            String phone = (String) requestData.get("phone");
            String email = (String) requestData.getOrDefault("email", "");
            String addressLine1 = (String) requestData.get("addressLine1");
            String addressLine2 = (String) requestData.getOrDefault("addressLine2", "");
            String landmark = (String) requestData.getOrDefault("landmark", "");
            String city = (String) requestData.get("city");
            String state = (String) requestData.get("state");
            String pincode = (String) requestData.get("pincode");
            String country = (String) requestData.getOrDefault("country", "India");

            String pickupSlot = (String) requestData.get("pickupSlot");
            String paymentMethod = (String) requestData.get("paymentMethod");

            // UPI details
            String upiName = (String) requestData.getOrDefault("upiName", "");
            String upiId = (String) requestData.getOrDefault("upiId", "");
            String upiPhone = (String) requestData.getOrDefault("upiPhone", "");

            // Bank details
            String bankAccountName = (String) requestData.getOrDefault("bankAccountName", "");
            String bankAccountNumber = (String) requestData.getOrDefault("bankAccountNumber", "");
            String ifscCode = (String) requestData.getOrDefault("ifscCode", "");

            // Prepare shipment payload for Delhivery
            Map<String, Object> shipment = new HashMap<>();
            shipment.put("name", name);
            shipment.put("add", addressLine1);
            shipment.put("city", city);
            shipment.put("state", state);
            shipment.put("pin", pincode);
            shipment.put("country", country);
            shipment.put("phone", phone);
            shipment.put("order", "SELL_" + System.currentTimeMillis());
            shipment.put("products_desc", bookTitle);
            shipment.put("total_amount", expectedPrice);
            shipment.put("payment_mode", "Pickup");
            shipment.put("cod_amount", 0);
            shipment.put("return_address", "F-228, Lado Sarai");
            shipment.put("return_type", "pickup");
            shipment.put("return_city", "Delhi");
            shipment.put("return_state", "Delhi");
            shipment.put("return_pin", "110030");
            shipment.put("return_country", "India");
            shipment.put("return_phone", "8447466860");
            shipment.put("shipping_mode", "Surface");
            shipment.put("weight", 200);
            shipment.put("length", 10);
            shipment.put("breadth", 8);
            shipment.put("height", 2);

            Map<String, Object> pickupLocation = Map.of(
                    "name", "ReRead Warehouse",
                    "address", "F-228, Lado Sarai",
                    "city", "Delhi",
                    "state", "Delhi",
                    "country", "India",
                    "pin", "110030",
                    "phone", "8447466860"
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("pickup_location", pickupLocation);
            payload.put("shipments", java.util.List.of(shipment));

            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = "format=json&data=" + URLEncoder.encode(payloadJson, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Token de98d0920680bab24a81d26e9f588e325dc20090");

            HttpEntity<String> delhiveryRequest = new HttpEntity<>(encodedPayload, headers);
            ResponseEntity<String> delhiveryResponse = restTemplate.postForEntity(
                    "https://track.delhivery.com/api/cmu/create.json",
                    delhiveryRequest,
                    String.class
            );

            System.out.println("Delhivery Raw Response: " + delhiveryResponse.getBody());

            JsonNode rootNode = objectMapper.readTree(delhiveryResponse.getBody());
            JsonNode packagesNode = rootNode.get("packages");

            String awbNumber = null;
            if (packagesNode != null && packagesNode.isArray() && packagesNode.size() > 0) {
                JsonNode packageNode = packagesNode.get(0);
                if (packageNode != null && packageNode.has("waybill")) {
                    awbNumber = packageNode.get("waybill").asText();
                } else {
                    return ResponseEntity.status(500).body(Map.of(
                            "error", "Delhivery response missing waybill",
                            "delhivery_response", delhiveryResponse.getBody()
                    ));
                }
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "error", "Delhivery response missing packages",
                        "delhivery_response", delhiveryResponse.getBody()
                ));
            }

            // Save sell order
            SellOrder sellOrder = new SellOrder();
            sellOrder.setName(name);
            sellOrder.setPhone(phone);
            sellOrder.setEmail(email);
            sellOrder.setAddressLine1(addressLine1);
            sellOrder.setAddressLine2(addressLine2);
            sellOrder.setCity(city);
            sellOrder.setState(state);
            sellOrder.setPincode(pincode);
            sellOrder.setCountry(country);
            sellOrder.setLandmark(landmark);
            sellOrder.setPickupSlot(pickupSlot);
            sellOrder.setPaymentMethod(paymentMethod);

            if ("UPI".equalsIgnoreCase(paymentMethod)) {
                sellOrder.setUpiName(upiName);
                sellOrder.setUpiId(upiId);
                sellOrder.setUpiPhone(upiPhone);
            } else if ("BANK".equalsIgnoreCase(paymentMethod)) {
                sellOrder.setBankAccountName(bankAccountName);
                sellOrder.setBankAccountNumber(bankAccountNumber);
                sellOrder.setIfscCode(ifscCode);
            }

            sellOrder.setAwb(awbNumber);
            sellOrder.setStatus("PENDING");

            sellOrderRepository.save(sellOrder);

            //  Send WhatsApp notification after saving
            String fullPickupAddress = addressLine1 + ", " +
                    (addressLine2.isEmpty() ? "" : addressLine2 + ", ") +
                    (landmark.isEmpty() ? "" : landmark + ", ") +
                    city + ", " + state + " - " + pincode;

            whatsappService.sendSellOrderConfirmation(
                    phone,
                    name,
                    bookTitle,
                    fullPickupAddress,
                    paymentMethod,
                    String.valueOf(expectedPrice)
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Sell order created successfully",
                    "awb", awbNumber,
                    "delhivery_response", delhiveryResponse.getBody()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
