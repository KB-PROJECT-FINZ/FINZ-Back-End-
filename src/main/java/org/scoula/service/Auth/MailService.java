package org.scoula.service.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
public abstract class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTemporaryPassword(String toEmail, String tempPassword) {
        try {
            // 현재 설정된 메일서버 확인용 로그
            JavaMailSenderImpl senderImpl = (JavaMailSenderImpl) mailSender;
            System.out.println("[DEBUG] SMTP Host: " + senderImpl.getHost());
            System.out.println("[DEBUG] SMTP Port: " + senderImpl.getPort());
            System.out.println("DEBUG] SMTP Username: " + senderImpl.getUsername());
            System.out.println("[DEBUG] SMTP Password: " + (senderImpl.getPassword() != null));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("portfolio0704@naver.com");
            helper.setTo(toEmail);
            helper.setSubject("[FINZ] 임시 비밀번호 안내");
            helper.setText(buildHtmlContent(tempPassword), true); // true → HTML 형식

            mailSender.send(message);

            System.out.println(" 메일 전송 성공 → " + toEmail);

        } catch (Exception e) {
            System.out.println(" 메일 전송 실패");
            e.printStackTrace();
        }
    }

    private String buildHtmlContent(String tempPassword) {
        return "<div style=\"padding:20px; font-family:Arial, sans-serif;\">" +
                "<h2>FINZ 비밀번호 재설정</h2>" +
                "<p>임시 비밀번호는 아래와 같습니다:</p>" +
                "<p style=\"font-size:20px; font-weight:bold; color:#4A90E2;\">" + tempPassword + "</p>" +
                "<p>로그인 후 반드시 비밀번호를 변경해주세요.</p>" +
                "<br><p>감사합니다. - FINZ</p>" +
                "</div>";
    }

    public abstract void sendResetPasswordEmail(String to, String tempPassword);
}