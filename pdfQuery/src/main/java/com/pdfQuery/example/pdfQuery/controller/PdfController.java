package com.pdfQuery.example.pdfQuery.controller;

import com.pdfQuery.example.pdfQuery.service.PdfQnAService;
import com.pdfQuery.example.pdfQuery.service.PdfStorage;
import com.pdfQuery.example.utils.PdfProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    private final PdfProcessor pdfProcessor;
    private final PdfQnAService pdfQnAService;
    private final PdfStorage pdfStorage;

    @Autowired
    public PdfController(PdfProcessor pdfProcessor, PdfQnAService pdfQnAService, PdfStorage pdfStorage) {
        this.pdfProcessor = pdfProcessor;
        this.pdfQnAService = pdfQnAService;
        this.pdfStorage = pdfStorage;
    }


    @PostMapping("/upload")
    public String uploadPdf(@RequestParam MultipartFile file) {
        try {
            File tempFile = convertMultipartFileToFile(file);
            String pdfContent = pdfProcessor.extractText(tempFile.getAbsolutePath());

            // Debug: Inspect raw PDF content
            System.out.println("Raw PDF content: " + pdfContent);

            String id = UUID.randomUUID().toString();
            Map<String, AbstractMap.SimpleEntry<String, String>> chunkEmbeddings = new HashMap<>();

            // Clean the content to remove non-printable characters
            String cleanedContent = pdfContent.replaceAll("[\\p{Cntrl}]+", " ").trim();
            System.out.println("Cleaned content: " + cleanedContent);

            // Split by paragraphs or fallback to sentences
            String[] initialChunks = cleanedContent.split("\\n{2,}");
            if (initialChunks.length == 1) {
                initialChunks = cleanedContent.split("(?<=[.!?])\\s+");
            }

            // Combine smaller chunks into larger ones
            List<String> optimizedChunks = new ArrayList<>();
            StringBuilder currentChunk = new StringBuilder();
            int minChunkSize = 500; // Minimum chunk size in characters

            for (String chunk : initialChunks) {
                if (currentChunk.length() + chunk.length() < minChunkSize) {
                    currentChunk.append(chunk).append(" ");
                } else {
                    optimizedChunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                    currentChunk.append(chunk);
                }
            }
            // Add the last chunk if not empty
            if (currentChunk.length() > 0) {
                optimizedChunks.add(currentChunk.toString().trim());
            }

            // Debug: Check the resulting optimized chunks
            System.out.println("Optimized Chunks: " + optimizedChunks);

            for (int i = 0; i < optimizedChunks.size(); i++) {
                String chunk = optimizedChunks.get(i).trim();
                if (!chunk.isEmpty()) {
                    String chunkEmbedding = pdfQnAService.generateEmbedding(chunk);

                    // Store both the chunk text and its embedding as a SimpleEntry
                    chunkEmbeddings.put(id + "::chunk_" + i, new AbstractMap.SimpleEntry<>(chunk, chunkEmbedding));
                }
            }
            System.out.println("Chunk Embeddings: " + chunkEmbeddings);

            // Save both the chunks and their embeddings to pdfStorage with the generated ID
            pdfStorage.save(id, chunkEmbeddings);

            return "PDF uploaded successfully. Use this ID for questions: " + id;

        } catch (IOException e) {
            return "Error processing PDF: " + e.getMessage();
        }
    }



    @GetMapping("/ask")
    public String askQuestion(@RequestParam String question, @RequestParam String pdfId) {
        long startTime = System.currentTimeMillis();

        long stepStart = System.currentTimeMillis();
        String questionVector = pdfQnAService.generateEmbedding(question);
        long stepEnd = System.currentTimeMillis();
        System.out.println("Execution time for pdfQnAService.generateEmbedding: " + (stepEnd - stepStart) + "ms");

        stepStart = System.currentTimeMillis();
        Map<String, AbstractMap.SimpleEntry<String, String>> pdfContent = pdfStorage.getAllById(pdfId);
        stepEnd = System.currentTimeMillis();
        System.out.println("Execution time for pdfStorage.getAllById: " + (stepEnd - stepStart) + "ms");

        stepStart = System.currentTimeMillis();
        String matchedContent = pdfQnAService.findBestMatch(pdfContent, questionVector);
        System.out.println("Matched Content: "+ matchedContent);
        stepEnd = System.currentTimeMillis();
        System.out.println("Execution time for pdfQnAService.findBestMatch: " + (stepEnd - stepStart) + "ms");

        stepStart = System.currentTimeMillis();
        String result = pdfQnAService.askQuestion(matchedContent, question);
        stepEnd = System.currentTimeMillis();
        System.out.println("Execution time for pdfQnAService.askQuestion: " + (stepEnd - stepStart) + "ms");

        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time of askQuestion method: " + (endTime - startTime) + "ms");

        return result;
    }



    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload-", ".pdf");
        file.transferTo(tempFile);
        return tempFile;
    }
}
