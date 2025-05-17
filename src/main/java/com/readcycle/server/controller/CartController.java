package com.readcycle.server.controller;

import com.readcycle.server.entity.CartItem;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.CartItemRepository;
import com.readcycle.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(cartItemRepository.findByUser(user));
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody CartItem item, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        item.setUser(user);
        return ResponseEntity.ok(cartItemRepository.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long id, Principal principal) {
        CartItem item = cartItemRepository.findById(id).orElseThrow();
        if (!item.getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).build();
        }
        cartItemRepository.delete(item);
        return ResponseEntity.ok().build();
    }
}
