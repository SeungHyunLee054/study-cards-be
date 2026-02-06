package com.example.study_cards.application.generation.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.generation.dto.request.ApprovalRequest;
import com.example.study_cards.application.generation.dto.request.GenerationRequest;
import com.example.study_cards.application.generation.service.GenerationService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import com.example.study_cards.domain.generation.repository.GeneratedCardRepository;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
class AdminGenerationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GeneratedCardRepository generatedCardRepository;

    @MockitoBean
    private AiGenerationService aiGenerationService;

    private String adminAccessToken;
    private String userAccessToken;
    private Category category;
    private GeneratedCard generatedCard;

    @BeforeEach
    void setUp() {
        generatedCardRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest adminSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "admin@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "adminUser")
                .sample();

        authService.signUp(adminSignUpRequest);
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        admin.addRole(Role.ROLE_ADMIN);
        admin.verifyEmail();
        userRepository.save(admin);

        SignInRequest adminSignInRequest = new SignInRequest("admin@example.com", "password123");
        TokenResult adminTokenResult = authService.signIn(adminSignInRequest);
        adminAccessToken = adminTokenResult.accessToken();

        SignUpRequest userSignUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "user@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "normalUser")
                .sample();

        authService.signUp(userSignUpRequest);
        verifyUserEmail("user@example.com");

        SignInRequest userSignInRequest = new SignInRequest("user@example.com", "password123");
        TokenResult userTokenResult = authService.signIn(userSignInRequest);
        userAccessToken = userTokenResult.accessToken();

        category = categoryRepository.save(Category.builder()
                .code("CS")
                .name("Computer Science")
                .build());

        generatedCard = generatedCardRepository.save(GeneratedCard.builder()
                .model("gpt-4")
                .sourceWord("Algorithm")
                .prompt("Generate a flashcard about algorithms")
                .question("What is an algorithm?")
                .questionSub("알고리즘이란?")
                .answer("A step-by-step procedure")
                .answerSub("단계별 절차")
                .category(category)
                .build());
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/admin/generation/stats")
    class GetStatsTest {

        @Test
        @DisplayName("관리자가 생성 통계를 조회한다")
        void getStats_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/generation/stats")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overall").exists())
                    .andExpect(jsonPath("$.byModel").isArray())
                    .andDo(document("admin-generation/get-stats",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            responseFields(
                                    fieldWithPath("overall").type(JsonFieldType.OBJECT).description("전체 통계"),
                                    fieldWithPath("overall.totalGenerated").type(JsonFieldType.NUMBER).description("총 생성 수"),
                                    fieldWithPath("overall.approved").type(JsonFieldType.NUMBER).description("승인 수"),
                                    fieldWithPath("overall.rejected").type(JsonFieldType.NUMBER).description("거절 수"),
                                    fieldWithPath("overall.pending").type(JsonFieldType.NUMBER).description("대기 수"),
                                    fieldWithPath("overall.migrated").type(JsonFieldType.NUMBER).description("이동 수"),
                                    fieldWithPath("overall.approvalRate").type(JsonFieldType.NUMBER).description("승인율"),
                                    fieldWithPath("byModel").type(JsonFieldType.ARRAY).description("모델별 통계"),
                                    fieldWithPath("byModel[].model").type(JsonFieldType.STRING).description("모델명"),
                                    fieldWithPath("byModel[].totalGenerated").type(JsonFieldType.NUMBER).description("총 생성 수"),
                                    fieldWithPath("byModel[].approved").type(JsonFieldType.NUMBER).description("승인 수"),
                                    fieldWithPath("byModel[].rejected").type(JsonFieldType.NUMBER).description("거절 수"),
                                    fieldWithPath("byModel[].pending").type(JsonFieldType.NUMBER).description("대기 수"),
                                    fieldWithPath("byModel[].migrated").type(JsonFieldType.NUMBER).description("이동 수"),
                                    fieldWithPath("byModel[].approvalRate").type(JsonFieldType.NUMBER).description("승인율")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void getStats_user_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/generation/stats")
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/generation/cards")
    class GetGeneratedCardsTest {

        @Test
        @DisplayName("관리자가 생성된 카드 목록을 조회한다")
        void getGeneratedCards_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/generation/cards")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andDo(document("admin-generation/get-generated-cards",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            queryParameters(
                                    parameterWithName("status").description("상태 필터 (PENDING, APPROVED, REJECTED, MIGRATED)").optional(),
                                    parameterWithName("model").description("모델 필터").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("상태로 필터링하여 조회한다")
        void getGeneratedCards_withStatusFilter_success() throws Exception {
            mockMvc.perform(get("/api/admin/generation/cards")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/generation/cards/{id}")
    class GetGeneratedCardTest {

        @Test
        @DisplayName("관리자가 단일 생성 카드를 조회한다")
        void getGeneratedCard_admin_success() throws Exception {
            mockMvc.perform(get("/api/admin/generation/cards/{id}", generatedCard.getId())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(generatedCard.getId()))
                    .andDo(document("admin-generation/get-generated-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("생성 카드 ID")
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("ID"),
                                    fieldWithPath("model").type(JsonFieldType.STRING).description("AI 모델"),
                                    fieldWithPath("sourceWord").type(JsonFieldType.STRING).description("소스 단어"),
                                    fieldWithPath("question").type(JsonFieldType.STRING).description("질문"),
                                    fieldWithPath("questionSub").type(JsonFieldType.STRING).description("질문 부연설명").optional(),
                                    fieldWithPath("answer").type(JsonFieldType.STRING).description("답변"),
                                    fieldWithPath("answerSub").type(JsonFieldType.STRING).description("답변 부연설명").optional(),
                                    fieldWithPath("category").type(JsonFieldType.OBJECT).description("카테고리 정보"),
                                    fieldWithPath("category.id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("category.code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("category.name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("category.parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("category.parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("status").type(JsonFieldType.STRING).description("상태"),
                                    fieldWithPath("approvedAt").type(JsonFieldType.STRING).description("승인일시").optional(),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성일시")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/generation/cards/{id}/approve")
    class ApproveTest {

        @Test
        @DisplayName("관리자가 카드를 승인한다")
        void approve_admin_success() throws Exception {
            mockMvc.perform(patch("/api/admin/generation/cards/{id}/approve", generatedCard.getId())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andDo(document("admin-generation/approve-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("생성 카드 ID")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void approve_user_returns403() throws Exception {
            mockMvc.perform(patch("/api/admin/generation/cards/{id}/approve", generatedCard.getId())
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/generation/cards/{id}/reject")
    class RejectTest {

        @Test
        @DisplayName("관리자가 카드를 거절한다")
        void reject_admin_success() throws Exception {
            mockMvc.perform(patch("/api/admin/generation/cards/{id}/reject", generatedCard.getId())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andDo(document("admin-generation/reject-card",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("생성 카드 ID")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/generation/cards/batch-approve")
    class BatchApproveTest {

        @Test
        @DisplayName("관리자가 여러 카드를 일괄 승인한다")
        void batchApprove_admin_success() throws Exception {
            ApprovalRequest request = new ApprovalRequest(List.of(generatedCard.getId()));

            mockMvc.perform(post("/api/admin/generation/cards/batch-approve")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andDo(document("admin-generation/batch-approve",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            requestFields(
                                    fieldWithPath("ids").type(JsonFieldType.ARRAY).description("승인할 카드 ID 목록")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void batchApprove_user_returns403() throws Exception {
            ApprovalRequest request = new ApprovalRequest(List.of(generatedCard.getId()));

            mockMvc.perform(post("/api/admin/generation/cards/batch-approve")
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/generation/migrate")
    class MigrateTest {

        @Test
        @DisplayName("관리자가 승인된 카드를 이동한다")
        void migrate_admin_success() throws Exception {
            generatedCard.approve();
            generatedCardRepository.save(generatedCard);

            mockMvc.perform(post("/api/admin/generation/migrate")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.migratedCount").isNumber())
                    .andExpect(jsonPath("$.message").exists())
                    .andDo(document("admin-generation/migrate",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            responseFields(
                                    fieldWithPath("migratedCount").type(JsonFieldType.NUMBER).description("이동된 카드 수"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void migrate_user_returns403() throws Exception {
            mockMvc.perform(post("/api/admin/generation/migrate")
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }
    }
}
