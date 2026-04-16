package com.actvn.enotary.controller;

import com.actvn.enotary.dto.response.VideoSessionResponse;
import com.actvn.enotary.service.VideoSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VideoSessionControllerTest {

    private MockMvc mockMvc;
    private VideoSessionService videoSessionService;

    @BeforeEach
    void setUp() {
        videoSessionService = Mockito.mock(VideoSessionService.class);
        VideoSessionController controller = new VideoSessionController(videoSessionService);
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "http://localhost:5173");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.actvn.enotary.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void getVideoRoom_withToken_redirectsToFrontendAndPassesToken() throws Exception {
        when(videoSessionService.verifySessionToken("token-123"))
                .thenReturn(VideoSessionResponse.builder().roomId("room_abcd").build());

        mockMvc.perform(get("/api/video/room/room_abcd").param("token", "token-123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/video/room/room_abcd?token=token-123"));

        verify(videoSessionService, times(1)).verifySessionToken("token-123");
    }

    @Test
    void getVideoRoom_withoutToken_redirectsToFrontendWithoutToken() throws Exception {
        mockMvc.perform(get("/api/video/room/room_xyz"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173/video/room/room_xyz"));

        verifyNoInteractions(videoSessionService);
    }
}

