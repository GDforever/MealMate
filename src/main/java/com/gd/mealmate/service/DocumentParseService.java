package com.gd.mealmate.service;

import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.model.enums.DocumentContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DocumentParseService {

    @Value("${app.knowledge.chunk-size:500}")
    private int chunkSize;

    @Value("${app.knowledge.chunk-overlap:50}")
    private int chunkOverlap;

    public String parseFile(Path filePath, DocumentContentType contentType) {
        try {
            return switch (contentType) {
                case TXT -> Files.readString(filePath, StandardCharsets.UTF_8);
                case PDF -> parsePdf(filePath);
                case DOCX -> parseDocx(filePath);
                default -> throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_PARSE_ERROR);
        }
    }

    private String parsePdf(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String paraText = para.getText();
                if (paraText != null && !paraText.isBlank()) {
                    text.append(paraText).append("\n");
                }
            }
            return text.toString();
        }
    }

    public List<String> splitIntoChunks(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int charChunkSize = chunkSize * 3 / 2;
        int charOverlap = chunkOverlap * 3 / 2;
        int pos = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + charChunkSize, text.length());
            String chunk = text.substring(pos, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            pos += charChunkSize - charOverlap;
            if (pos >= text.length()) break;
        }

        return chunks;
    }
}
