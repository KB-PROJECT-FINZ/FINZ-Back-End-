package org.scoula.service.Auth;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl extends MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendResetPasswordEmail(String to, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[FINZ] 비밀번호 재설정 안내");
        message.setText("안녕하세요.\n\n요청하신 임시 비밀번호는 다음과 같습니다:\n\n" +
                tempPassword +
                "\n\n로그인 후 반드시 비밀번호를 변경해 주세요.\n감사합니다.");

        mailSender.send(message);
    }
}
