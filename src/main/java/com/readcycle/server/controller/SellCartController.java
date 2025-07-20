package com.readcycle.server.controller;

import com.readcycle.server.entity.SellCartItem;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.SellCartItemRepository;
import com.readcycle.server.repository.UserRepository;
import com.readcycle.server.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sell-cart")
public class SellCartController {

    private final SellCartItemRepository sellCartItemRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public SellCartController(SellCartItemRepository sellCartItemRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.sellCartItemRepository = sellCartItemRepository;
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
    public List<SellCartItem> getUserSellCart(HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        return sellCartItemRepository.findByUser(user);
    }

    @PostMapping
    public SellCartItem addToSellCart(@RequestBody SellCartItem item, HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        item.setUser(user);
        return sellCartItemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    public void deleteSellCartItem(@PathVariable Long id, HttpServletRequest request) {
        User user = extractUserFromRequest(request);

        SellCartItem item = sellCartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sell cart item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this sell cart item");
        }

        sellCartItemRepository.delete(item);
    }

    @DeleteMapping("/clear")
    public void clearSellCart(HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        sellCartItemRepository.deleteByUser(user);
    }
}
