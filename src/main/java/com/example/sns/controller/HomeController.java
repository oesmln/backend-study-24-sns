package com.example.sns.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(
            HttpServletRequest request,
            Model model
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");

        model.addAttribute("message", "인증에 성공했습니다.");
        model.addAttribute("userId", userId);
        model.addAttribute("email", email);

        return "home";
    }
}