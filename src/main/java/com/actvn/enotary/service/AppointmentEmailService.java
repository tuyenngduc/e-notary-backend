package com.actvn.enotary.service;

import com.actvn.enotary.entity.Appointment;
import com.actvn.enotary.entity.NotaryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class AppointmentEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    public AppointmentEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendOnlineMeetingLinkToClient(NotaryRequest request, Appointment appointment) {
        if (!emailEnabled) {
            return;
        }
        if (request == null || request.getClient() == null || request.getClient().getEmail() == null || request.getClient().getEmail().isBlank()) {
            log.warn("Skip meeting-link email: missing client email");
            return;
        }
        if (appointment == null || appointment.getMeetingUrl() == null || appointment.getMeetingUrl().isBlank()) {
            log.warn("Skip meeting-link email: missing meeting URL");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Skip meeting-link email: JavaMailSender is not configured");
            return;
        }

        String clientEmail = request.getClient().getEmail();
        String subject = "[E-Notary] Xác nhận lịch hẹn công chứng trực tuyến";
        OffsetDateTime time = appointment.getScheduledTime();
        String formattedTime = time != null
                ? time.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Chưa xác định";

        String body = "Kính gửi Quý khách,\n\n"
                + "Chúng tôi xin xác nhận lịch hẹn công chứng trực tuyến của bạn đã được thiết lập thành công.\n\n"

                + "📅 Thời gian hẹn: " + formattedTime + "\n"
                + "🔗 Link tham gia: " + appointment.getMeetingUrl() + "\n\n"

                + "Vui lòng truy cập đường dẫn trên đúng giờ để bắt đầu phiên làm việc với công chứng viên.\n"
                + "Để đảm bảo chất lượng phiên họp, vui lòng kiểm tra trước kết nối internet, camera và micro.\n\n"

                + "Nếu bạn cần hỗ trợ, vui lòng liên hệ với chúng tôi qua email này.\n\n"

                + "Trân trọng,\n"
                + "Hệ thống E-Notary";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(clientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Meeting-link email sent to client={}", clientEmail);
        } catch (Exception ex) {
            // Keep scheduling flow successful even if email delivery fails.
            log.error("Failed to send meeting-link email to client={}", clientEmail, ex);
        }
    }
}

