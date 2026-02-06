package com.example.study_cards.application.category.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.application.category.dto.request.CategoryCreateRequest;
import com.example.study_cards.application.category.dto.request.CategoryUpdateRequest;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.user.entity.Role;
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
class AdminCategoryControllerTest extends BaseIntegrationTest {

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

    private String adminAccessToken;
    private String userAccessToken;
    private Category category;

    @BeforeEach
    void setUp() {
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
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("POST /api/admin/categories")
    class CreateCategoryTest {

        @Test
        @DisplayName("관리자가 카테고리를 생성한다")
        void createCategory_admin_success() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "ALGO",
                    "Algorithm",
                    "CS",
                    1
            );

            mockMvc.perform(post("/api/admin/categories")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("ALGO"))
                    .andExpect(jsonPath("$.name").value("Algorithm"))
                    .andExpect(jsonPath("$.parentCode").value("CS"))
                    .andDo(document("admin-category/create-category",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            requestFields(
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("표시 순서").optional()
                            ),
                            responseFields(
                                    fieldWithPath("id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("루트 카테고리를 생성한다")
        void createCategory_root_success() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "MATH",
                    "Mathematics",
                    null,
                    2
            );

            mockMvc.perform(post("/api/admin/categories")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("MATH"))
                    .andExpect(jsonPath("$.parentCode").isEmpty());
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void createCategory_user_returns403() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "NEW", "New Category", null, 1
            );

            mockMvc.perform(post("/api/admin/categories")
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("코드가 비어있으면 400을 반환한다")
        void createCategory_blankCode_returns400() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "", "Name", null, 1
            );

            mockMvc.perform(post("/api/admin/categories")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이름이 비어있으면 400을 반환한다")
        void createCategory_blankName_returns400() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(
                    "CODE", "", null, 1
            );

            mockMvc.perform(post("/api/admin/categories")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/categories/{id}")
    class UpdateCategoryTest {

        @Test
        @DisplayName("관리자가 카테고리를 수정한다")
        void updateCategory_admin_success() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest(
                    "CS_UPDATED",
                    "Computer Science Updated",
                    2
            );

            mockMvc.perform(put("/api/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("CS_UPDATED"))
                    .andExpect(jsonPath("$.name").value("Computer Science Updated"))
                    .andDo(document("admin-category/update-category",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("카테고리 ID")
                            ),
                            requestFields(
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("표시 순서").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void updateCategory_user_returns403() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest(
                    "UPDATED", "Updated", 1
            );

            mockMvc.perform(put("/api/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + userAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/categories/{id}")
    class DeleteCategoryTest {

        @Test
        @DisplayName("관리자가 카테고리를 삭제한다")
        void deleteCategory_admin_success() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent())
                    .andDo(document("admin-category/delete-category",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰 (ADMIN 권한 필요)")
                            ),
                            pathParameters(
                                    parameterWithName("id").description("카테고리 ID")
                            )
                    ));
        }

        @Test
        @DisplayName("일반 사용자가 요청하면 403을 반환한다")
        void deleteCategory_user_returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + userAccessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void deleteCategory_unauthorized_returns401() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/{id}", category.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
