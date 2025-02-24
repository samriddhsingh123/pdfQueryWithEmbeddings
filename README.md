# PDF Query App

## Overview

PDF Query is a Spring Boot-based application that enables users to upload PDFs and ask questions about their content. The system processes PDFs into meaningful chunks, generates vector embeddings using OpenAI, and retrieves the most relevant sections to answer user queries using weighted similarity techniques.

## Features

- **PDF Upload**: Users can upload multiple PDFs via a REST API.
- **Text Chunking & Embeddings**: PDFs are broken into meaningful chunks, and embeddings are generated using OpenAI's API.
- **Query Processing**: User questions are converted into vector embeddings and matched against stored chunks using cosine similarity and TF-IDF weighting.
- **Answer Generation**: The system retrieves the most relevant chunks and sends them as context to a Generative AI model (OLLAMA) to generate answers.

## Technologies Used

- **Backend**: Spring Boot, Spring AI
- **Vector Embeddings**: OpenAI API, Python script using `allenai/longformer-base-4096`
- **AI Model**: OLLAMA (runs locally)
- **Storage & Retrieval**: Cosine similarity, TF-IDF weighting, priority queue for ranking
- **Server Configuration**:
  - `server.port=8080`
  - `spring.servlet.multipart.max-file-size=10MB`
  - `spring.servlet.multipart.max-request-size=10MB`
  - `spring.application.name=pdfQuery`
  - `spring.ai.ollama.base-url=http://localhost:11434`
  - `spring.ai.ollama.chat.enabled=true`
  - `spring.ai.ollama.chat.options.model=tinyllama`

## How It Works

1. **PDF Upload**: Users send a POST request with a PDF file.
2. **Processing**:
   - The PDF is split into chunks.
   - Vector embeddings are generated using OpenAI's API.
   - Chunks and embeddings are stored in a map.
3. **Query Execution**: Users send a GET request with their question.
4. **Similarity Computation**:
   - The question is converted to an embedding.
   - Stop words are removed, and TF-IDF is computed.
   - Cosine similarity and TF-IDF weighting are used to rank the most relevant chunks.
5. **Answer Generation**: The top-ranked chunks are sent as context to the OLLAMA AI model, which generates the final answer.

## Setup & Running Locally

1. Install dependencies for Spring Boot.
2. Ensure OLLAMA is running locally (`http://localhost:11434`).
3. Run the Python script for embeddings.
4. Start the Spring Boot application (`server.port=8080`).
5. Use Postman or any REST client to interact with the API.

---

## Resume Summary

Developed a Spring Boot-based PDF Q&A system for NatWest Group, integrating Spring AI, OpenAI embeddings, and OLLAMA for local AI inference. The system processes PDFs into structured chunks, generates embeddings, and retrieves relevant sections using TF-IDF and cosine similarity, ensuring accurate context retrieval for user queries.



