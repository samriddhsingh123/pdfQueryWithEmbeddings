package com.pdfQuery.example.pdfQuery.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;

import java.net.URI;
import java.util.*;
import java.util.AbstractMap;
import org.springframework.web.client.RestTemplate;

@Service
public class PdfQnAService {

    private static final String OLLAMA_API_URL = "http://localhost:11411/v1/embeddings";
    private final ChatClient chatClient;
    private final RestTemplate restTemplate;
    private final PdfStorage pdfStorage;

    @Autowired
    public PdfQnAService(RestTemplate restTemplate, PdfStorage pdfStorage, ChatClient.Builder chatClientBuilder) {
        this.restTemplate = restTemplate;
        this.pdfStorage = pdfStorage;
        this.chatClient = chatClientBuilder.build(); // Ensure chatClient is initialized
    }

    public String askQuestion(String pdfContent, String question) {
        String fullPrompt = "PDF Content:\n" + pdfContent + "\n\nQuestion: " + question;
        return chatClient.prompt()
                .user(fullPrompt)
                .call()
                .content();
    }

    public String generateEmbedding(String content) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            content = cleanText(content);

            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(Map.of("text", content));

            URI uri = UriComponentsBuilder.fromUriString(OLLAMA_API_URL).build().toUri();
            RequestEntity<String> request = new RequestEntity<>(requestBody, headers, HttpMethod.POST, uri);

            String response = restTemplate.exchange(request, String.class).getBody();

            ObjectMapper responseMapper = new ObjectMapper();
            JsonNode jsonResponse = responseMapper.readTree(response);
            return jsonResponse.path("embedding").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating embedding: " + e.getMessage();
        }
    }

    private RealVector normalizeVector(RealVector vector) {
        double norm = vector.getNorm();
        return norm == 0 ? vector : vector.mapDivide(norm);
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when", "at", "by", "for",
            "in", "of", "on", "to", "with", "is", "was", "are", "were", "be", "been", "being", "have",
            "has", "had", "do", "does", "did", "not", "no", "yes", "it", "this", "that", "these", "those"
    );

    private String removeStopWords(String text) {
        StringBuilder filteredText = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!STOP_WORDS.contains(word.toLowerCase())) {
                filteredText.append(word).append(" ");
            }
        }
        return filteredText.toString().trim();
    }

    // TF-IDF Calculation for the question to weight important tokens
    private Map<String, Double> calculateTFIDF(String question, List<String> allPdfChunks) {
        String filteredQuestion = removeStopWords(question);

        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : filteredQuestion.split("\\s+")) {
            termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
        }

        Map<String, Double> tfidf = new HashMap<>();
        int numChunks = allPdfChunks.size();

        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String token = entry.getKey();
            int docFrequency = 0;
            for (String chunk : allPdfChunks) {
                if (chunk.contains(token)) {
                    docFrequency++;
                }
            }
            double idf = Math.log((double) numChunks / (docFrequency + 1));
            tfidf.put(token, entry.getValue() * idf);
        }
        return tfidf;
    }


    public String findBestMatch(Map<String, AbstractMap.SimpleEntry<String, String>> pdfContent, String questionVector, String question) {
        RealVector questionVec = normalizeVector(parseVector(questionVector));
        PriorityQueue<Map.Entry<String, Double>> topMatches = new PriorityQueue<>(Map.Entry.comparingByValue());

        int topN = 2;
        double similarityThreshold = 0.6;

        // Collect all chunks for TF-IDF calculation
        List<String> allChunks = new ArrayList<>();
        for (Map.Entry<String, AbstractMap.SimpleEntry<String, String>> entry : pdfContent.entrySet()) {
            allChunks.add(entry.getValue().getKey());
        }

        // Calculate TF-IDF for the question
        Map<String, Double> tfidfScores = calculateTFIDF(question, allChunks);
        System.out.println("TF-IDF Scores for the question:");

        for (Map.Entry<String, Double> entry : tfidfScores.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        double questionWeight = 1.5; // Factor to weight question relevance

        for (Map.Entry<String, AbstractMap.SimpleEntry<String, String>> entry : pdfContent.entrySet()) {
            String chunkContent = entry.getValue().getKey();
            String chunkEmbedding = entry.getValue().getValue();
            System.out.println("Chunk Content2: " + chunkContent);

            RealVector chunkVec = normalizeVector(parseVector(chunkEmbedding));
            double similarity = cosineSimilarity(questionVec, chunkVec);

            // Apply the TF-IDF weight for each relevant term in the chunk
            double tfidfFactor = 0.0;
            for (String token : chunkContent.split("\\s+")) {
                if (tfidfScores.containsKey(token)) {
                    tfidfFactor += tfidfScores.get(token);
                }
            }

            // Weighted cosine similarity considering TF-IDF
            double weightedSimilarity = similarity * questionWeight * (1 + tfidfFactor);

            if (weightedSimilarity > similarityThreshold) {
                topMatches.offer(Map.entry(chunkContent, weightedSimilarity));
                if (topMatches.size() > topN) {
                    topMatches.poll();
                }
            }
        }

        if (topMatches.isEmpty()) {
            return "No close match found.";
        }

        StringBuilder aggregatedContext = new StringBuilder();
        while (!topMatches.isEmpty()) {
            Map.Entry<String, Double> match = topMatches.poll();
            aggregatedContext.insert(0, match.getKey() + " ");
        }

        return aggregatedContext.toString().trim();
    }


    private String cleanText(String content) {
        return content.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private RealVector parseVector(String vectorString) {
        try {
            vectorString = vectorString.replace("[[", "").replace("]]", "");
            String[] values = vectorString.split(",");
            double[] vectorArray = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                vectorArray[i] = Double.parseDouble(values[i].trim());
            }
            return new ArrayRealVector(vectorArray);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayRealVector(new double[0]);
        }
    }

    private double cosineSimilarity(RealVector vector1, RealVector vector2) {
        return vector1.dotProduct(vector2) / (vector1.getNorm() * vector2.getNorm());
    }
}
