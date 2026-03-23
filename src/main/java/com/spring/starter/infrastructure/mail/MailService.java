package com.spring.starter.infrastructure.mail;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;

/**
 * Transactional email service using Spring Mail + Thymeleaf HTML templates.
 *
 * <p>
 * All send methods are {@code @Async} so they don't block the request thread.
 * Templates live in {@code resources/templates/mail/}.
 * In dev, emails are captured by the MailHog container (http://localhost:8025).
 */
@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private static final String FROM_ADDRESS = "noreply@hdcamp.vn";

    private static String resolveSubject(String purpose) {
        return switch (purpose) {
            case "verify-email" -> "Xác thực email — HDCamp";
            case "reset-password" -> "Đặt lại mật khẩu — HDCamp";
            default -> "HDCamp — Thông báo";
        };
    }

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an OTP email for email verification or password reset.
     *
     * @param to      recipient email address
     * @param otpCode the 6-digit OTP code
     * @param purpose "verify-email" | "reset-password" — used to select the
     *                template
     */
    @Async
    public void sendOtpEmail(String to, String otpCode, String purpose) {
        var variables = Map.of(
                "otpCode", otpCode,
                "expiryMinutes", "5");
        sendHtmlEmail(to, resolveSubject(purpose), "mail/otp", variables);
    }

    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, ?> variables) {
        try {
            var context = new Context();
            context.setVariables(Map.copyOf(variables));

            var html = templateEngine.process(templateName, context);

            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(FROM_ADDRESS);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            logger.debug("Sent email [{}] to {}", subject, to);
        } catch (MessagingException ex) {
            // Log and swallow — email failures should not break the main request flow
            logger.error("Failed to send email [{}] to {}: {}", subject, to, ex.getMessage());
        }
    }
}