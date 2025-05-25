package com.readcycle.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcycle.server.entity.Address;
import com.readcycle.server.entity.Order;
import com.readcycle.server.entity.OrderItem;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.AddressRepository;
import com.readcycle.server.repository.CartItemRepository;
import com.readcycle.server.repository.OrderRepository;
import com.readcycle.server.repository.UserRepository;
import com.readcycle.server.security.JwtUtil;
import com.razorpay.RazorpayClient;
import com.readcycle.server.util.SSLUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final JwtUtil jwtUtil;

    private RazorpayClient razorpayClient;

    {
        try {
            razorpayClient = new RazorpayClient("rzp_test_fLdHPGEAL3ijP6", "dTMvSdX663pm23V25U460UtT");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String userId = jwtUtil.validateAndGetUserId(token);
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUser(user);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Order> placeOrder(@RequestBody Order order, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String userId = jwtUtil.validateAndGetUserId(token);
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        order.setUser(user);
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
            }
        }

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteByUser(user);

        return ResponseEntity.ok(savedOrder);
    }

    @PostMapping("/create-razorpay-order")
    @Transactional
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(@RequestBody Map<String, Object> data, HttpServletRequest request) {

        try {

            SSLUtil.disableSSLVerification();
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAndGetUserId(token);
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            int amount = (int) data.get("amount");

            Map<String, Object> optionsMap = new HashMap<>();
            optionsMap.put("amount", amount);
            optionsMap.put("currency", "INR");
            optionsMap.put("receipt", "order_rcptid_" + System.currentTimeMillis());
            optionsMap.put("payment_capture", 1);

            JSONObject options = new JSONObject(optionsMap);
            com.razorpay.Order razorpayOrder = razorpayClient.Orders.create(options);

            // 🌐 Delhivery Integration
            ObjectMapper objectMapper = new ObjectMapper();
            List<Address> addresses = addressRepository.findByUser(user);

            Optional<Address> defaultAddressOpt = addresses.stream()
                    .filter(Address::isDefault)
                    .findFirst();

            Address defaultAddress = defaultAddressOpt.orElseGet(() -> {
                Address fallback = addresses.get(0);
                fallback.setDefault(true);
                return addressRepository.save(fallback);
            });

            Map<String, Object> pickupLocation = Map.of(
                    "name", "ReRead Warehouse",
                    "address", "F-228, Lado Sarai",
                    "city", "Delhi",
                    "state", "Delhi",
                    "country", "India",
                    "pin", "110030",
                    "phone", "8447466860"
            );

            Map<String, Object> shipment = new HashMap<>();
            shipment.put("order", "ORDER_" + System.currentTimeMillis());
            shipment.put("products_desc", "Book purchase");
            shipment.put("cod_amount", 0);
            shipment.put("total_amount", amount / 100.0);
            shipment.put("name", defaultAddress.getName());
            shipment.put("add", defaultAddress.getStreet());
            shipment.put("city", defaultAddress.getCity());
            shipment.put("state", defaultAddress.getState());
            shipment.put("country", defaultAddress.getCountry());
            shipment.put("pin", defaultAddress.getPostalCode());
            shipment.put("phone", defaultAddress.getPhone());
            shipment.put("payment_mode", "Prepaid");
            shipment.put("shipping_mode", "Surface");
            shipment.put("weight", 150);
            shipment.put("length", 10);
            shipment.put("breadth", 8);
            shipment.put("height", 2);
            shipment.put("return_address", "F-288, Lado Sarai");
            shipment.put("return_pin", "110030");
            shipment.put("return_city", "Delhi");
            shipment.put("return_state", "Delhi");
            shipment.put("return_country", "India");
            shipment.put("return_phone", "8447466860");

            Map<String, Object> payload = new HashMap<>();
            payload.put("pickup_location", pickupLocation);
            payload.put("shipments", List.of(shipment));

            String payloadJson = objectMapper.writeValueAsString(payload);
            System.out.println("📦 Delhivery Payload JSON: " + payloadJson); // <-- Logging the payload

            String encodedPayload = "format=json&data=" +
                    URLEncoder.encode(payloadJson, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Token de98d0920680bab24a81d26e9f588e325dc20090");

            HttpEntity<String> requestEntity = new HttpEntity<>(encodedPayload, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> delhiveryResponse = restTemplate.postForEntity(
                    "https://track.delhivery.com/api/cmu/create.json",
                    requestEntity,
                    String.class
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", razorpayOrder.get("id"));
            response.put("amount", razorpayOrder.get("amount"));
            response.put("currency", razorpayOrder.get("currency"));
            response.put("delhivery_response", delhiveryResponse.getBody());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    @Transactional
    public ResponseEntity<?> verifyPaymentAndSaveOrder(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAndGetUserId(token);
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String razorpayOrderId = (String) payload.get("razorpay_order_id");
            String razorpayPaymentId = (String) payload.get("razorpay_payment_id");
            String razorpaySignature = (String) payload.get("razorpay_signature");

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean isValid = com.razorpay.Utils.verifyPaymentSignature(attributes, "dTMvSdX663pm23V25U460UtT");
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid payment signature"));
            }

            Order order = new Order();
            order.setUser(user);
            order.setStatus("COMPLETED");
            order.setOrderDate(LocalDateTime.now());

            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) payload.get("items");
            if (itemsList != null) {
                for (Map<String, Object> itemMap : itemsList) {
                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setBookId(Long.parseLong(itemMap.get("bookId").toString()));
                    item.setTitle(itemMap.get("title").toString());
                    item.setAuthor(itemMap.get("author").toString());
                    item.setPrice(Double.parseDouble(itemMap.get("price").toString()));
                    item.setQuantity(Integer.parseInt(itemMap.get("quantity").toString()));
                    order.getItems().add(item);
                }
            }

            order.setTotalAmount(Double.parseDouble(payload.get("totalAmount").toString()));
            Order savedOrder = orderRepository.save(order);
            cartItemRepository.deleteByUser(user);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to verify payment and save order", "error", e.getMessage()));
        }
    }
}
