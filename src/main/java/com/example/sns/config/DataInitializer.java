package com.example.sns.config;

import com.example.sns.entity.User;
import com.example.sns.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// @Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        User user = User.create("test@test.com", "테스트유저", "1234");

        userRepository.save(user);
    }
}