package com.almousleck.service;

import com.almousleck.dto.OtpResponse;

public interface OtpService {
    OtpResponse generateOtp(String phoneNumber);
    void verifyOtp(String phoneNumber, String optCode);
    OtpResponse resendOpt(String phoneNumber);
}
