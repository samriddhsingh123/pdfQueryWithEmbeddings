package com.pdfQuery.example.pdfQuery.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;

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

//    public PdfQnAService(ChatClient.Builder chatClientBuilder) {
//        this.restTemplate = new RestTemplate(); // Provide a default initialization
//        this.pdfStorage = null; // Initialize as null or inject an appropriate instance
//        this.chatClient = chatClientBuilder.build(); // Initialize chatClient
//    }

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
        double norm = vector.getNorm(); // Compute the magnitude of the vector
        return norm == 0 ? vector : vector.mapDivide(norm); // Divide each element by the norm
    }

    public String findBestMatch(Map<String, AbstractMap.SimpleEntry<String, String>> pdfContent, String questionVector) {
        RealVector questionVec = normalizeVector(parseVector(questionVector));
        PriorityQueue<Map.Entry<String, Double>> topMatches = new PriorityQueue<>(Map.Entry.comparingByValue());

        int topN = 3;
        double similarityThreshold = 0.3;

        for (Map.Entry<String, AbstractMap.SimpleEntry<String, String>> entry : pdfContent.entrySet()) {
            String chunkContent = entry.getValue().getKey();
            String chunkEmbedding = entry.getValue().getValue();

            RealVector chunkVec = normalizeVector(parseVector(chunkEmbedding));
            double similarity = cosineSimilarity(questionVec, chunkVec);

            if (similarity > similarityThreshold) {
                System.out.println("chunk vector: "+chunkVec);
                topMatches.offer(Map.entry(chunkContent, similarity));
                if (topMatches.size() > topN) {
                    topMatches.poll();
                }
            }
        }

        StringBuilder aggregatedContext = new StringBuilder();
        while (!topMatches.isEmpty()) {
            Map.Entry<String, Double> match = topMatches.poll();
            aggregatedContext.append(match.getKey()).append(" ");
        }

        return aggregatedContext.length() > 0 ? aggregatedContext.toString().trim() : "No close match found.";
    }




    private String[] splitIntoChunks(String content) {
        return content.split("(?<=[.!?])\\s+");
    }


    public String getAnswerFromLLM(String question, String matchedContent) {
        return "Answer based on matched content: " + matchedContent;
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

    private String cleanText(String content) {
        return content.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}
