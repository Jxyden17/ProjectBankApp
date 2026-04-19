package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.auth.request.LoginRequest;
import nl.donniebankoebarkie.api.dto.auth.request.RegisterRequest;
import nl.donniebankoebarkie.api.service.result.LoginResult;
import nl.donniebankoebarkie.api.service.result.RefreshResult;

public interface IAuthService {
    UserResponse register(RegisterRequest request);

    LoginResult login(LoginRequest request);

    RefreshResult refresh(String refreshToken);

    void logout(String refreshToken);

    UserResponse getCurrentUser(Long userId);
}
