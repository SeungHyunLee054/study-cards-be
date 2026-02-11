package com.example.study_cards.application.notification.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.notification.dto.request.FcmTokenRequest;
import com.example.study_cards.application.notification.dto.request.PushSettingRequest;
import com.example.study_cards.domain.notification.entity.Notification;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.notification.repository.NotificationRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class NotificationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String accessToken;
    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "testUser")
                .sample();

        authService.signUp(signUpRequest);
        user = userRepository.findByEmail("test@example.com").orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);

        SignInRequest signInRequest = new SignInRequest("test@example.com", "password123");
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.DAILY_REVIEW)
                .title("Test Notification")
                .body("This is a test notification")
                .build());
    }

    @Nested
    @DisplayName("POST /api/notifications/fcm-token")
    class RegisterFcmTokenTest {

        @Test
        @DisplayName("FCM 토큰을 등록한다")
        void registerFcmToken_success() throws Exception {
            FcmTokenRequest request = new FcmTokenRequest("fcm_token_abc123");

            mockMvc.perform(post("/api/notifications/fcm-token")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(document("notification/register-fcm-token",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("fcmToken").type(JsonFieldType.STRING).description("FCM 토큰")
                            )
                    ));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void registerFcmToken_unauthorized_returns401() throws Exception {
            FcmTokenRequest request = new FcmTokenRequest("fcm_token_abc123");

            mockMvc.perform(post("/api/notifications/fcm-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/notifications/fcm-token")
    class RemoveFcmTokenTest {

        @Test
        @DisplayName("FCM 토큰을 삭제한다")
        void removeFcmToken_success() throws Exception {
            mockMvc.perform(delete("/api/notifications/fcm-token")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("notification/remove-fcm-token",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/settings")
    class GetPushSettingsTest {

        @Test
        @DisplayName("푸시 설정을 조회한다")
        void getPushSettings_success() throws Exception {
            mockMvc.perform(get("/api/notifications/settings")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pushEnabled").isBoolean())
                    .andDo(document("notification/get-push-settings",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("pushEnabled").type(JsonFieldType.BOOLEAN).description("푸시 알림 활성화 여부"),
                                    fieldWithPath("hasFcmToken").type(JsonFieldType.BOOLEAN).description("FCM 토큰 보유 여부")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/settings")
    class UpdatePushSettingsTest {

        @Test
        @DisplayName("푸시 설정을 수정한다")
        void updatePushSettings_success() throws Exception {
            PushSettingRequest request = new PushSettingRequest(true);

            mockMvc.perform(patch("/api/notifications/settings")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(document("notification/update-push-settings",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            requestFields(
                                    fieldWithPath("pushEnabled").type(JsonFieldType.BOOLEAN).description("푸시 알림 활성화 여부")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/notifications")
    class GetNotificationsTest {

        @Test
        @DisplayName("알림 목록을 조회한다")
        void getNotifications_success() throws Exception {
            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("notification/get-notifications",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("content[].id").type(JsonFieldType.NUMBER).description("알림 ID"),
                                    fieldWithPath("content[].type").type(JsonFieldType.STRING).description("알림 타입"),
                                    fieldWithPath("content[].title").type(JsonFieldType.STRING).description("제목"),
                                    fieldWithPath("content[].body").type(JsonFieldType.STRING).description("내용"),
                                    fieldWithPath("content[].isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                                    fieldWithPath("content[].referenceId").type(JsonFieldType.NUMBER).description("참조 ID").optional(),
                                    fieldWithPath("content[].createdAt").type(JsonFieldType.STRING).description("생성일시")
                            ).and(subsectionWithPath("pageable").ignored(),
                                    fieldWithPath("totalElements").ignored(),
                                    fieldWithPath("totalPages").ignored(),
                                    fieldWithPath("size").ignored(),
                                    fieldWithPath("number").ignored(),
                                    fieldWithPath("first").ignored(),
                                    fieldWithPath("last").ignored(),
                                    subsectionWithPath("sort").ignored(),
                                    fieldWithPath("numberOfElements").ignored(),
                                    fieldWithPath("empty").ignored())
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread")
    class GetUnreadNotificationsTest {

        @Test
        @DisplayName("읽지 않은 알림을 조회한다")
        void getUnreadNotifications_success() throws Exception {
            mockMvc.perform(get("/api/notifications/unread")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("notification/get-unread-notifications",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread/count")
    class GetUnreadCountTest {

        @Test
        @DisplayName("읽지 않은 알림 수를 조회한다")
        void getUnreadCount_success() throws Exception {
            mockMvc.perform(get("/api/notifications/unread/count")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").isNumber())
                    .andDo(document("notification/get-unread-count",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("count").type(JsonFieldType.NUMBER).description("읽지 않은 알림 수")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{id}/read")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림을 읽음 처리한다")
        void markAsRead_success() throws Exception {
            mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andDo(document("notification/mark-as-read",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("알림 ID")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/read-all")
    class MarkAllAsReadTest {

        @Test
        @DisplayName("모든 알림을 읽음 처리한다")
        void markAllAsRead_success() throws Exception {
            mockMvc.perform(patch("/api/notifications/read-all")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andDo(document("notification/mark-all-as-read",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            )
                    ));
        }
    }
}
