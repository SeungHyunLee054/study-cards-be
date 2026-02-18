package com.example.study_cards.application.ai.service;

import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.support.BaseUnitTest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AiSourceTextExtractorServiceUnitTest extends BaseUnitTest {

    private final AiSourceTextExtractorService extractorService = new AiSourceTextExtractorService();

    @Nested
    @DisplayName("extractText")
    class ExtractTextTest {

        @Test
        @DisplayName("null 파일이면 예외를 던진다")
        void extractText_nullFile_throwsException() {
            assertThatThrownBy(() -> extractorService.extractText(null))
                    .isInstanceOf(AiException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_EXTRACTED_TEXT);
        }

        @Test
        @DisplayName("빈 파일이면 예외를 던진다")
        void extractText_emptyFile_throwsException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "note.txt",
                    "text/plain",
                    new byte[0]
            );

            assertThatThrownBy(() -> extractorService.extractText(file))
                    .isInstanceOf(AiException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_EXTRACTED_TEXT);
        }

        @Test
        @DisplayName("텍스트 파일은 내용을 추출한다")
        void extractText_plainText_success() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "note.md",
                    "text/markdown",
                    "  운영체제는 자원 관리 소프트웨어다.  ".getBytes(StandardCharsets.UTF_8)
            );

            String result = extractorService.extractText(file);

            assertThat(result).isEqualTo("운영체제는 자원 관리 소프트웨어다.");
        }

        @Test
        @DisplayName("PDF 파일은 내용을 추출한다")
        void extractText_pdf_success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "note.pdf",
                    "application/pdf",
                    createPdfBytes("PDF sample text")
            );

            String result = extractorService.extractText(file);

            assertThat(result).contains("PDF sample text");
        }

        @Test
        @DisplayName("입력 길이가 5000자를 넘으면 잘라서 반환한다")
        void extractText_tooLong_returnsTruncatedText() {
            String longText = "a".repeat(5100);
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "note.txt",
                    "text/plain",
                    longText.getBytes()
            );

            String result = extractorService.extractText(file);

            assertThat(result).hasSize(5000);
        }

        @Test
        @DisplayName("지원하지 않는 파일 형식이면 예외를 던진다")
        void extractText_unsupportedFile_throwsException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "note.exe",
                    "application/octet-stream",
                    "binary".getBytes()
            );

            assertThatThrownBy(() -> extractorService.extractText(file))
                    .isInstanceOf(AiException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        @Test
        @DisplayName("파일 크기가 제한을 초과하면 예외를 던진다")
        void extractText_fileSizeExceeded_throwsException() {
            ReflectionTestUtils.setField(extractorService, "maxUploadFileSizeBytes", 10L);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "note.txt",
                    "text/plain",
                    "12345678901".getBytes(StandardCharsets.UTF_8)
            );

            assertThatThrownBy(() -> extractorService.extractText(file))
                    .isInstanceOf(AiException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.FILE_SIZE_EXCEEDED);
        }

        @Test
        @DisplayName("텍스트 파일 읽기 실패 시 예외를 던진다")
        void extractText_plainTextReadFailed_throwsException() throws Exception {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getContentType()).willReturn("text/plain");
            given(file.getOriginalFilename()).willReturn("note.txt");
            given(file.getBytes()).willThrow(new IOException("read fail"));

            assertThatThrownBy(() -> extractorService.extractText(file))
                    .isInstanceOf(AiException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.FILE_TEXT_EXTRACTION_FAILED);
        }
    }

    private byte[] createPdfBytes(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }
}
