package com.almousleck.config;

import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
       User user = userRepository.findByUsernameOrPhoneNumber(identifier, identifier)
               .orElseThrow(() -> new UsernameNotFoundException("未找到用户名为“:”的用户: " + identifier));
       return ApplicationUserDetails.buildApplicationDetails(user);
    }
}
