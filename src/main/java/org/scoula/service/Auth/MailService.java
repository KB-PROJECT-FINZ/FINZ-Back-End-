package org.scoula.service.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTemporaryPassword(String toEmail, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[FINZIE] 임시 비밀번호 안내");
            helper.setText(buildHtmlContent(tempPassword), true); // true → HTML 형식

            mailSender.send(message);

            System.out.println("✅ 메일 전송 성공 → " + toEmail);

        } catch (Exception e) {
            System.out.println("❌ 메일 전송 실패");
            e.printStackTrace();
        }
    }

    private String buildHtmlContent(String tempPassword) {
        return "<div style=\"padding:20px; font-family:Arial, sans-serif;\">" +
                "<h2>FINZIE 비밀번호 재설정</h2>" +
                "<p>임시 비밀번호는 아래와 같습니다:</p>" +
                "<p style=\"font-size:20px; font-weight:bold; color:#4A90E2;\">" + tempPassword + "</p>" +
                "<p>로그인 후 반드시 비밀번호를 변경해주세요.</p>" +
                "<br><p>감사합니다. - FINZIE 팀</p>" +
                "</div>";
    }
}