package com.actvn.enotary.video;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VideoSignalingServiceTest {

    @Test
    void rejoinWithSameEmail_doesNotEvictActiveParticipantWhenOldSessionCloses() {
        VideoSignalingService service = new VideoSignalingService();

        WebSocketSession session1 = mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("s1");

        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("s2");

        WebSocketSession sessionB = mock(WebSocketSession.class);
        when(sessionB.getId()).thenReturn("sb");

        VideoSignalingService.JoinResult firstJoin = service.join("room", "a@example.com", session1);
        assertThat(firstJoin.participantCount()).isEqualTo(1);
        assertThat(firstJoin.offererEmail()).isEqualTo("a@example.com");

        VideoSignalingService.JoinResult rejoin = service.join("room", "a@example.com", session2);
        assertThat(rejoin.participantCount()).isEqualTo(1);
        assertThat(rejoin.offererEmail()).isEqualTo("a@example.com");

        // Old session closes after user refreshes/rejoins -> must not remove the active session.
        assertThat(service.leave(session1)).isNull();

        VideoSignalingService.JoinResult joinSecond = service.join("room", "b@example.com", sessionB);
        assertThat(joinSecond.participantCount()).isEqualTo(2);
        assertThat(joinSecond.offererEmail()).isEqualTo("a@example.com");

        VideoSignalingService.LeaveResult leaveActive = service.leave(session2);
        assertThat(leaveActive).isNotNull();
        assertThat(leaveActive.roomId()).isEqualTo("room");
        assertThat(leaveActive.email()).isEqualTo("a@example.com");
        assertThat(leaveActive.remainingSessions()).containsExactly(sessionB);
    }
}

