package com.pdfQuery.example.pdfQuery.service;

//import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PdfStorage {

    // Change the storage map to store a Pair (chunkContent, chunkEmbedding)
    private final ConcurrentHashMap<String, AbstractMap.SimpleEntry<String, String>> storage = new ConcurrentHashMap<>(); // Store chunks and their embeddings

    public void save(String pdfId, Map<String, AbstractMap.SimpleEntry<String, String>> chunks) {
        chunks.forEach((chunkId, chunkContent) -> {
            storage.put(chunkId, chunkContent); // Storing the embedding (value)
        });
    }

    // Retrieve chunks by pdfId, returning the chunk content and its embedding
    public Map<String, AbstractMap.SimpleEntry<String, String>> getChunksByPdfId(String pdfId) {
        return storage.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(pdfId + "::"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Retrieve the Pair for a specific chunkId (which contains both chunk content and its embedding)
    public AbstractMap.SimpleEntry<String, String> get(String id) {
        return storage.get(id); // Retrieve the Pair (chunkContent, chunkEmbedding)
    }

    // Check if a chunk exists by its ID
    public boolean exists(String id) {
        return storage.containsKey(id);
    }
//
//    // Print the storage with both chunk content and embedding
//    public void printStorage() {
//        storage.forEach((id, pair) -> {
//            System.out.println("ID: " + id + " - Chunk: " + pair.getLeft() + " - Embedding: " + pair.getRight());
//        });
//    }

    public Map<String, AbstractMap.SimpleEntry<String, String>> getAllById(String pdfId) {
        // Implement logic to retrieve all chunks for a given ID
        // Assuming storage stores chunks mapped to IDs and each entry is a Pair (chunkContent, chunkEmbedding)
        return storage.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(pdfId)) // Filter entries based on pdfId
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // Use the chunk ID as the key
                        entry -> new AbstractMap.SimpleEntry<>(entry.getValue().getKey(), entry.getValue().getValue()) // Create SimpleEntry with chunk content and embedding
                ));
    }




    // Getter for the storage map (useful for testing or debugging)
    public ConcurrentHashMap<String, AbstractMap.SimpleEntry<String, String>> getStorage() {
        return storage;
    }
}
