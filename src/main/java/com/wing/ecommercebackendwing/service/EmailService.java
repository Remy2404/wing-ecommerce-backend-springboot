package com.wing.ecommercebackendwing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Email Verification - Wing E-Commerce");
            message.setText(buildVerificationEmailContent(token));
            
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset - Wing E-Commerce");
            message.setText(buildPasswordResetEmailContent(token));
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Async
    public void sendAccountLockedEmail(String toEmail, String unlockTime) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Account Locked - Wing E-Commerce");
            message.setText(buildAccountLockedEmailContent(unlockTime));
            
            mailSender.send(message);
            log.info("Account locked email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send account locked email to: {}", toEmail, e);
        }
    }

    private String buildVerificationEmailContent(String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        return String.format(
            "Welcome to Wing E-Commerce!\n\n" +
            "Please verify your email address by clicking the link below:\n\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you didn't create an account, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Wing E-Commerce Team",
            verificationUrl
        );
    }

    private String buildPasswordResetEmailContent(String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        return String.format(
            "Hi,\n\n" +
            "We received a request to reset your password. Click the link below to reset it:\n\n" +
            "%s\n\n" +
            "This link will expire in 1 hour.\n\n" +
            "If you didn't request a password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Wing E-Commerce Team",
            resetUrl
        );
    }

    private String buildAccountLockedEmailContent(String unlockTime) {
        return String.format(
            "Hi,\n\n" +
            "Your account has been temporarily locked due to multiple failed login attempts.\n\n" +
            "Your account will be automatically unlocked at: %s\n\n" +
            "If you didn't attempt to login, please contact our support team immediately.\n\n" +
            "Best regards,\n" +
            "Wing E-Commerce Team",
            unlockTime
        );
    }
}
