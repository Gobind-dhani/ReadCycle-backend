package com.readcycle.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcycle.server.entity.*;
import com.readcycle.server.repository.*;
import com.readcycle.server.security.JwtUtil;
import com.readcycle.server.util.SSLUtil;
import com.razorpay.RazorpayClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

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

            List<Address> addresses = addressRepository.findByUser(user);

            Optional<Address> defaultAddressOpt = addresses.stream()
                    .filter(Address::isDefault)
                    .findFirst();

            Address defaultAddress = defaultAddressOpt.orElseGet(() -> {
                Address fallback = addresses.get(0);
                fallback.setDefault(true);
                return addressRepository.save(fallback);
            });

            List<CartItem> cartItems = cartItemRepository.findByUser(user);
            String productDescription = cartItems.stream()
                    .map(CartItem::getTitle)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Books");

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
            shipment.put("order", razorpayOrder.get("id"));
            shipment.put("products_desc", productDescription);
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
            String encodedPayload = "format=json&data=" + URLEncoder.encode(payloadJson, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Token de98d0920680bab24a81d26e9f588e325dc20090");

            HttpEntity<String> requestEntity = new HttpEntity<>(encodedPayload, headers);
            ResponseEntity<String> delhiveryResponse = restTemplate.postForEntity(
                    "https://track.delhivery.com/api/cmu/create.json",
                    requestEntity,
                    String.class
            );

            String awbNumber = null;
            JsonNode delhiveryJson = objectMapper.readTree(delhiveryResponse.getBody());
            if (delhiveryJson.has("packages") && delhiveryJson.get("packages").isArray()) {
                JsonNode packageNode = delhiveryJson.get("packages").get(0);
                if (packageNode.has("waybill")) {
                    awbNumber = packageNode.get("waybill").asText();
                }
            }

            // Construct and save final Order
            Order order = new Order();
            order.setUser(user);
            order.setRazorpayOrderId(razorpayOrder.get("id"));
            order.setAwb(awbNumber);
            order.setStatus("PENDING");
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(amount / 100.0);

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                OrderItem item = new OrderItem();
                item.setBookId(cartItem.getBookId());
                item.setTitle(cartItem.getTitle());
                item.setPrice(cartItem.getPrice());
                item.setQuantity(cartItem.getQuantity());
                item.setOrder(order); // important for FK
                orderItems.add(item);
            }

            order.setItems(orderItems);
            orderRepository.save(order);

            // Clear the cart
            cartItemRepository.deleteByUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("razorpay_order_id", razorpayOrder.get("id"));
            response.put("amount", razorpayOrder.get("amount"));
            response.put("currency", razorpayOrder.get("currency"));
            response.put("awb", awbNumber);
            response.put("delhivery_response", delhiveryResponse.getBody());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 🚚 Order Tracking Endpoint
    @GetMapping("/track/{awb}")
    public ResponseEntity<?> trackByAwb(@PathVariable String awb, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAndGetUserId(token);

            Optional<Order> optionalOrder = orderRepository.findByAwb(awb);
            if (optionalOrder.isEmpty() || !optionalOrder.get().getUser().getId().equals(Long.parseLong(userId))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found or not authorized");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token de98d0920680bab24a81d26e9f588e325dc20090");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = "https://track.delhivery.com/api/v1/packages/json/?waybill=" + awb;

            ResponseEntity<String> trackingResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(trackingResponse.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching tracking data: " + e.getMessage());
        }
    }

    @GetMapping("/download-label/{awb}")
    public ResponseEntity<byte[]> downloadShippingLabel(@PathVariable String awb, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAndGetUserId(token);

            Optional<Order> optionalOrder = orderRepository.findByAwb(awb);
            if (optionalOrder.isEmpty() || !optionalOrder.get().getUser().getId().equals(Long.parseLong(userId))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String labelApiUrl = "https://track.delhivery.com/api/p/packing_slip?wbns=" + awb + "&pdf=true";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token de98d0920680bab24a81d26e9f588e325dc20090");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> delhiveryResponse = restTemplate.exchange(
                    labelApiUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (delhiveryResponse.getStatusCode() != HttpStatus.OK) {
                return ResponseEntity.status(delhiveryResponse.getStatusCode()).body(null);
            }

            JsonNode rootNode = objectMapper.readTree(delhiveryResponse.getBody());
            JsonNode packagesNode = rootNode.path("packages");

            if (!packagesNode.isArray() || packagesNode.size() == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String pdfUrl = packagesNode.get(0).path("pdf_download_link").asText();

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(pdfUrl);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    byte[] pdfBytes = EntityUtils.toByteArray(response.getEntity());

                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.setContentType(MediaType.APPLICATION_PDF);
                    responseHeaders.setContentDisposition(ContentDisposition.builder("attachment")
                            .filename("shipping-label-" + awb + ".pdf")
                            .build());

                    return new ResponseEntity<>(pdfBytes, responseHeaders, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(response.getStatusLine().getStatusCode()).body(null);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}