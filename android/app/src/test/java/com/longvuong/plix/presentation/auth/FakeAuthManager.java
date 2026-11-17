package com.longvuong.plix.presentation.auth;

import com.longvuong.plix.core.auth.AuthManager;

//AuthManager giả: không gọi mạng, test đặt loggedIn để điều khiển isLoggedIn()
public class FakeAuthManager extends AuthManager {
    public boolean loggedIn;

    public FakeAuthManager() {
        super(null);
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }
}