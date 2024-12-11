package com.pubsub.service;

import com.pubsub.model.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Set;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendContentNotification(String recipientEmail, Content content, Set<String> matchedKeywords) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setSubject("New Content Matching Your Subscriptions: " + content.getTitle());

            String emailContent = buildEmailContent(content, matchedKeywords);
            helper.setText(emailContent, true); 

            mailSender.send(message);
            logger.info("Email notification sent to {} for content: {}", recipientEmail, content.getTitle());
        } catch (MessagingException e) {
            logger.error("Failed to send email notification to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    private String buildEmailContent(Content content, Set<String> matchedKeywords) {
        return String.format("""
                        <html>
                        <body>
                    <h2>Nouveau contenu correspondant à vos abonnements</h2>
                    <div style='margin: 20px 0;'>
                        <h3>%s</h3>
                        <p><strong>Publisher:</strong> %s</p>
                        <p><strong>Published at:</strong> %s</p>
                        <p><strong>Matched Keywords:</strong> %s</p>
                        <div style='border: 1px solid #ccc; padding: 10px;'>
                            <p>%s</p>
                        </div>
                        <p><strong>Keywords:</strong> %s</p>
                    </div>
                    <p style='font-size: 12px;'>
                        Ceci est un message automatique de notification du système de pubsub du contenu.
                    </p>
                </body>
                        </html>
                        """,
                content.getTitle(),
                content.getPublisherId(),
                content.getCreatedAt(),
                String.join(", ", matchedKeywords),
                content.getBody(),
                String.join(", ", content.getKeywords()));
    }
}
