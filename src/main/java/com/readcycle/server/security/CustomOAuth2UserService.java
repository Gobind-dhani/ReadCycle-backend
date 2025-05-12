package com.readcycle.server.security;

import com.readcycle.server.entity.User;
import com.readcycle.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google" or "github"
        String providerId;
        String name;
        String email;
        String avatarUrl;

        if (provider.equals("google")) {
            providerId = oAuth2User.getAttribute("sub");
            name = oAuth2User.getAttribute("name");
            email = oAuth2User.getAttribute("email");
            avatarUrl = oAuth2User.getAttribute("picture");
        } else if (provider.equals("github")) {
            providerId = oAuth2User.getAttribute("id").toString();
            name = oAuth2User.getAttribute("name");
            email = oAuth2User.getAttribute("email");
            avatarUrl = oAuth2User.getAttribute("avatar_url");
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        }

        userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User newUser = new User(null, providerId, provider, name, email, avatarUrl);
                    return userRepository.save(newUser);
                });

        return oAuth2User;
    }
}
