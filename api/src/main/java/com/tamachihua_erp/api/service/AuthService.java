package com.tamachihua_erp.api.service;

import com.tamachihua_erp.api.dto.auth.LoginRequestDto;
import com.tamachihua_erp.api.dto.auth.LoginResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request, String clientInterface);
}
