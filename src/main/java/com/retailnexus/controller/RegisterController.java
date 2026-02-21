package com.retailnexus.controller;

import com.retailnexus.entity.User;
import com.retailnexus.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/register")
@Controller
public class RegisterController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterDto());
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registerForm") RegisterDto dto, BindingResult result,
                           RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "register";
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match.");
            return "register";
        }
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            result.rejectValue("username", "error.username", "Username already exists.");
            return "register";
        }
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.CASHIER);
        userRepository.save(user);
        ra.addFlashAttribute("message", "Registration successful. Please login.");
        return "redirect:/login";
    }

    public static class RegisterDto {
        private String username;
        private String password;
        private String confirmPassword;

        @jakarta.validation.constraints.NotBlank(message = "Username is required")
        @jakarta.validation.constraints.Size(min = 3, max = 50)
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        @jakarta.validation.constraints.NotBlank(message = "Password is required")
        @jakarta.validation.constraints.Size(min = 6, message = "Password must be at least 6 characters")
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        @jakarta.validation.constraints.NotBlank(message = "Please confirm password")
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
