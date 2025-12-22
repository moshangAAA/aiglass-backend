package com.almousleck.config;

import com.almousleck.exceptions.PhoneNotVerifiedException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService  loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Check if the account is locked
        loginAttemptService.checkAccountLock(identifier);

       User user = userRepository.findByUsernameOrPhoneNumber(identifier, identifier)
               .orElseThrow(() -> new UsernameNotFoundException("未找到用户名为“:”的用户: " + identifier));

       // check if the phone is verified
        if (!user.getPhoneVerified())
            throw new PhoneNotVerifiedException("手机号未验证，请先验证手机号后再登录");

       return ApplicationUserDetails.buildApplicationDetails(user);
    }
}
