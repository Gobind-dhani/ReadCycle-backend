package com.readcycle.server.security;

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

    // Replace this with your actual frontend URL
    private final String FRONTEND_REDIRECT_URL = "http://localhost:3000/";

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

        String token = jwtUtil.generateToken(user.getId().toString());

        // Redirect to frontend with token in URL
        String redirectUrl = "http://localhost:3000/oauth2/redirect"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
