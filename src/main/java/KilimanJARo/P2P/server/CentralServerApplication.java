package KilimanJARo.P2P.server;

import KilimanJARo.P2P.server.monitors.UserConnectionMonitor;
import KilimanJARo.P2P.server.requests.AuthRequest;
import KilimanJARo.P2P.server.requests.LogoutRequest;
import KilimanJARo.P2P.server.requests.RegisterRequest;
import KilimanJARo.P2P.server.responses.AuthResponse;
import KilimanJARo.P2P.server.responses.LogoutResponse;
import KilimanJARo.P2P.server.responses.RegisterResponse;
import KilimanJARo.P2P.user.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SpringBootApplication
public class CentralServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CentralServerApplication.class, "--spring.config.name=central_server");
    }

    @Bean(name="centralSecurityChain")
    public SecurityFilterChain securityFilterChainMain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
