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
import java.util.Arrays;
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

        int charChunkSize = chunkSize * 3 / 2;
        int charOverlap = chunkOverlap * 3 / 2;

        List<String> paragraphs = Arrays.stream(text.split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> sentences = new ArrayList<>();
        for (String para : paragraphs) {
            sentences.addAll(splitIntoSentences(para, charChunkSize));
        }

        return mergeIntoChunks(sentences, charChunkSize, charOverlap);
    }

    private List<String> splitIntoSentences(String text, int maxSize) {
        List<String> sentences = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            if (isSentenceEnd(text.charAt(i))) {
                String sentence = text.substring(start, i + 1).trim();
                if (!sentence.isEmpty()) {
                    if (sentence.length() > maxSize) {
                        sentences.addAll(splitByFixedSize(sentence, maxSize));
                    } else {
                        sentences.add(sentence);
                    }
                }
                start = i + 1;
            }
        }

        if (start < text.length()) {
            String remaining = text.substring(start).trim();
            if (!remaining.isEmpty()) {
                if (remaining.length() > maxSize) {
                    sentences.addAll(splitByFixedSize(remaining, maxSize));
                } else {
                    sentences.add(remaining);
                }
            }
        }

        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            sentences.addAll(splitByFixedSize(text.trim(), maxSize));
        }

        return sentences;
    }

    private boolean isSentenceEnd(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；'
                || c == '.' || c == '!' || c == '?' || c == ';'
                || c == '\n';
    }

    private List<String> splitByFixedSize(String text, int size) {
        List<String> result = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            int end = Math.min(pos + size, text.length());
            String chunk = text.substring(pos, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
            pos = end;
        }
        return result;
    }

    private List<String> mergeIntoChunks(List<String> segments, int chunkSize, int overlap) {
        if (segments.isEmpty()) return List.of();

        List<String> chunks = new ArrayList<>();
        int segStart = 0;

        while (segStart < segments.size()) {
            StringBuilder chunk = new StringBuilder();
            int segEnd = segStart;

            while (segEnd < segments.size()) {
                String seg = segments.get(segEnd);
                if (chunk.length() > 0 && chunk.length() + seg.length() > chunkSize) {
                    break;
                }
                chunk.append(seg);
                segEnd++;
            }

            chunks.add(chunk.toString().trim());

            if (segEnd >= segments.size()) break;

            int newStart = segEnd;
            int overlapLen = 0;
            while (newStart > segStart + 1 && overlapLen < overlap) {
                newStart--;
                overlapLen += segments.get(newStart).length();
            }
            segStart = newStart;
        }

        return chunks;
    }
}
