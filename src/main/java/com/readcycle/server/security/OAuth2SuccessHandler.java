package com.readcycle.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final String FRONTEND_REDIRECT_URL = "https://readcycle.in/oauth2/redirect";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String provider = request.getRequestURI().contains("github") ? "github" : "google";
        String providerId;

        if (provider.equals("google")) {
            providerId = oAuth2User.getAttribute("sub");
        } else if (provider.equals("github")) {
            providerId = oAuth2User.getAttribute("id").toString();
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        }

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getPhone());


        // Serialize user object to JSON and encode it for URL
        String userJson = objectMapper.writeValueAsString(user);
        String encodedUserJson = URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        // Redirect with token and encoded user JSON
        String redirectUrl = FRONTEND_REDIRECT_URL
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&user=" + encodedUserJson;

        response.sendRedirect(redirectUrl);
    }
}
