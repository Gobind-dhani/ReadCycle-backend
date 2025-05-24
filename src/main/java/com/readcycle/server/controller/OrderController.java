package com.readcycle.server.controller;

import com.readcycle.server.entity.Order;
import com.readcycle.server.entity.OrderItem;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.CartItemRepository;
import com.readcycle.server.repository.OrderRepository;
import com.readcycle.server.repository.UserRepository;
import com.readcycle.server.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import com.razorpay.RazorpayClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final JwtUtil jwtUtil;

    private RazorpayClient razorpayClient;

    {
        try {
            // Initialize Razorpay client with your test keys (replace with env/config)
            razorpayClient = new RazorpayClient("rzp_test_fLdHPGEAL3ijP6", "dTMvSdX663pm23V25U460UtT");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    // GET orders for authenticated user
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

    // POST order for authenticated user (existing logic)
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

    // NEW: Create Razorpay Order (called from frontend before payment)


    @PostMapping("/create-razorpay-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(@RequestBody Map<String, Object> data) {
        try {
            int amount = (int) data.get("amount"); // amount in paise

            Map<String, Object> optionsMap = new HashMap<>();
            optionsMap.put("amount", amount);
            optionsMap.put("currency", "INR");
            optionsMap.put("receipt", "order_rcptid_" + System.currentTimeMillis());
            optionsMap.put("payment_capture", 1);

            // Convert Map to JSONObject
            JSONObject options = new JSONObject(optionsMap);

            // Use fully qualified class name and pass JSONObject
            com.razorpay.Order razorpayOrder = razorpayClient.Orders.create(options);

            Map<String, Object> response = new HashMap<>();
            response.put("id", razorpayOrder.get("id"));
            response.put("amount", razorpayOrder.get("amount"));
            response.put("currency", razorpayOrder.get("currency"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Verify Razorpay Payment Signature and save order
    @PostMapping("/verify-payment")
    @Transactional
    public ResponseEntity<?> verifyPaymentAndSaveOrder(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            // Validate JWT and get user
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

            // Verify signature
            boolean isValid = com.razorpay.Utils.verifyPaymentSignature(attributes, "your_razorpay_secret");
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid payment signature"));
            }

            // Save order
            Order order = new Order();
            order.setUser(user);
            order.setStatus("COMPLETED");
            order.setOrderDate(java.time.LocalDateTime.now());

            // Items expected as List<Map<String,Object>> in payload
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

            // Clear cart
            cartItemRepository.deleteByUser(user);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to verify payment and save order", "error", e.getMessage()));
        }
    }
}
