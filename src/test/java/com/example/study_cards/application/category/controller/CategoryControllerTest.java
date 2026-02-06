package com.example.study_cards.application.category.controller;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.TokenResult;
import com.example.study_cards.application.auth.service.AuthService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class CategoryControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private String accessToken;
    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        SignUpRequest signUpRequest = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
                .set("email", "test@example.com")
                .set("password", "password123")
                .set("passwordConfirm", "password123")
                .set("nickname", "testUser")
                .sample();

        authService.signUp(signUpRequest);
        verifyUserEmail("test@example.com");

        SignInRequest signInRequest = new SignInRequest("test@example.com", "password123");
        TokenResult tokenResult = authService.signIn(signInRequest);
        accessToken = tokenResult.accessToken();

        rootCategory = categoryRepository.save(Category.builder()
                .code("CS")
                .name("Computer Science")
                .displayOrder(1)
                .build());

        childCategory = categoryRepository.save(Category.builder()
                .code("ALGO")
                .name("Algorithm")
                .parent(rootCategory)
                .displayOrder(1)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    private void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.verifyEmail();
        userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("모든 카테고리를 조회한다")
        void getAllCategories_success() throws Exception {
            mockMvc.perform(get("/api/categories")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].code").value("CS"))
                    .andDo(document("category/get-all-categories",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("카테고리 목록"),
                                    fieldWithPath("content[].id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("content[].code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("content[].name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("content[].parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("content[].parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                    fieldWithPath("pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                                    fieldWithPath("pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지 여부"),
                                    fieldWithPath("pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지 아님 여부"),
                                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                    fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("number").type(JsonFieldType.NUMBER).description("페이지 번호"),
                                    fieldWithPath("sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                    fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                    fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("비어있음 여부")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/categories/tree")
    class GetCategoryTreeTest {

        @Test
        @DisplayName("카테고리 트리를 조회한다")
        void getCategoryTree_success() throws Exception {
            mockMvc.perform(get("/api/categories/tree")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").value("CS"))
                    .andDo(document("category/get-category-tree",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("[].code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("[].name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("[].depth").type(JsonFieldType.NUMBER).description("카테고리 깊이"),
                                    fieldWithPath("[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서"),
                                    fieldWithPath("[].children").type(JsonFieldType.ARRAY).description("자식 카테고리 목록"),
                                    fieldWithPath("[].children[].id").type(JsonFieldType.NUMBER).description("자식 카테고리 ID"),
                                    fieldWithPath("[].children[].code").type(JsonFieldType.STRING).description("자식 카테고리 코드"),
                                    fieldWithPath("[].children[].name").type(JsonFieldType.STRING).description("자식 카테고리 이름"),
                                    fieldWithPath("[].children[].depth").type(JsonFieldType.NUMBER).description("자식 카테고리 깊이"),
                                    fieldWithPath("[].children[].displayOrder").type(JsonFieldType.NUMBER).description("자식 카테고리 표시 순서"),
                                    fieldWithPath("[].children[].children").type(JsonFieldType.ARRAY).description("자식의 자식 카테고리 목록")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{code}")
    class GetCategoryTest {

        @Test
        @DisplayName("코드로 카테고리를 조회한다")
        void getCategory_success() throws Exception {
            mockMvc.perform(get("/api/categories/{code}", "CS")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("CS"))
                    .andExpect(jsonPath("$.name").value("Computer Science"))
                    .andDo(document("category/get-category",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("code").description("카테고리 코드")
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
        @DisplayName("존재하지 않는 카테고리 조회 시 404를 반환한다")
        void getCategory_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/categories/{code}", "NONEXISTENT")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{code}/children")
    class GetChildCategoriesTest {

        @Test
        @DisplayName("자식 카테고리를 조회한다")
        void getChildCategories_success() throws Exception {
            mockMvc.perform(get("/api/categories/{code}/children", "CS")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].code").value("ALGO"))
                    .andDo(document("category/get-child-categories",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 액세스 토큰")
                            ),
                            pathParameters(
                                    parameterWithName("code").description("부모 카테고리 코드")
                            ),
                            responseFields(
                                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("자식 카테고리 목록"),
                                    fieldWithPath("content[].id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                    fieldWithPath("content[].code").type(JsonFieldType.STRING).description("카테고리 코드"),
                                    fieldWithPath("content[].name").type(JsonFieldType.STRING).description("카테고리 이름"),
                                    fieldWithPath("content[].parentId").type(JsonFieldType.NUMBER).description("부모 카테고리 ID").optional(),
                                    fieldWithPath("content[].parentCode").type(JsonFieldType.STRING).description("부모 카테고리 코드").optional(),
                                    fieldWithPath("pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                    fieldWithPath("pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                                    fieldWithPath("pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지 여부"),
                                    fieldWithPath("pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지 아님 여부"),
                                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                    fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                    fieldWithPath("number").type(JsonFieldType.NUMBER).description("페이지 번호"),
                                    fieldWithPath("sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                                    fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 비어있음 여부"),
                                    fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                    fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),
                                    fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                    fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                    fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("비어있음 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 부모 카테고리 조회 시 404를 반환한다")
        void getChildCategories_parentNotFound_returns404() throws Exception {
            mockMvc.perform(get("/api/categories/{code}/children", "NONEXISTENT")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }
    }
}
