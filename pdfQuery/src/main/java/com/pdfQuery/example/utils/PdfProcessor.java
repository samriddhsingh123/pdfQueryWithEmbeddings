package com.pdfQuery.example.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.Loader;
import java.io.File;
import java.io.IOException;

@Component
public class PdfProcessor {
    public String extractText(String filePath) throws IOException {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();


            return stripper.getText(document);
        }
    }
}
