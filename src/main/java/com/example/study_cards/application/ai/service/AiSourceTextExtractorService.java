package com.example.study_cards.application.ai.service;

import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Service
public class AiSourceTextExtractorService {

    private static final long DEFAULT_MAX_UPLOAD_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_SOURCE_LENGTH = 5000;

    @Value("${app.ai.upload.max-file-size-bytes:5242880}")
    private long maxUploadFileSizeBytes = DEFAULT_MAX_UPLOAD_FILE_SIZE_BYTES;

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AiException(AiErrorCode.EMPTY_EXTRACTED_TEXT);
        }

        long maxUploadSize = normalizeMaxUploadFileSizeBytes();
        if (file.getSize() > maxUploadSize) {
            throw new AiException(AiErrorCode.FILE_SIZE_EXCEEDED);
        }

        String extracted;
        if (isPdf(file)) {
            extracted = extractPdfText(file);
        } else if (isPlainText(file)) {
            extracted = extractPlainText(file);
        } else {
            throw new AiException(AiErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        String normalized = extracted == null ? "" : extracted.trim();
        if (normalized.isBlank()) {
            throw new AiException(AiErrorCode.EMPTY_EXTRACTED_TEXT);
        }

        if (normalized.length() > MAX_SOURCE_LENGTH) {
            return normalized.substring(0, MAX_SOURCE_LENGTH);
        }
        return normalized;
    }

    private long normalizeMaxUploadFileSizeBytes() {
        if (maxUploadFileSizeBytes < 1) {
            log.warn("잘못된 파일 업로드 제한 설정(app.ai.upload.max-file-size-bytes={}), 기본값({})으로 보정합니다.",
                    maxUploadFileSizeBytes, DEFAULT_MAX_UPLOAD_FILE_SIZE_BYTES);
            return DEFAULT_MAX_UPLOAD_FILE_SIZE_BYTES;
        }
        return maxUploadFileSizeBytes;
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return "application/pdf".equalsIgnoreCase(contentType)
                || (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf"));
    }

    private boolean isPlainText(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        if (contentType != null) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("text/")) {
                return true;
            }
            if ("application/octet-stream".equals(normalized)) {
                return hasTextExtension(filename);
            }
        }
        return hasTextExtension(filename);
    }

    private boolean hasTextExtension(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt")
                || lower.endsWith(".md")
                || lower.endsWith(".markdown");
    }

    private String extractPlainText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[AI] 텍스트 파일 파싱 실패: {}", e.getMessage());
            throw new AiException(AiErrorCode.FILE_TEXT_EXTRACTION_FAILED);
        }
    }

    private String extractPdfText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            log.warn("[AI] PDF 텍스트 추출 실패: {}", e.getMessage());
            throw new AiException(AiErrorCode.FILE_TEXT_EXTRACTION_FAILED);
        }
    }
}
