package com.readcycle.server.controller;

import com.readcycle.server.entity.CartItem;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.CartItemRepository;
import com.readcycle.server.repository.UserRepository;
import com.readcycle.server.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CartController(CartItemRepository cartItemRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    private User extractUserFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String userId = jwtUtil.validateAndGetUserId(token);
        return userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<CartItem> getUserCart(HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        return cartItemRepository.findByUser(user);
    }

    @PostMapping
    public CartItem addToCart(@RequestBody CartItem item, HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        item.setUser(user);
        return cartItemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    public void deleteCartItem(@PathVariable Long id, HttpServletRequest request) {
        User user = extractUserFromRequest(request);

        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this cart item");
        }

        cartItemRepository.delete(item);
    }
}
