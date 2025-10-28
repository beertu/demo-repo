package com.dtas.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered test results analyzer
 * @author Jan Robert Bodino
 */
public class AIAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIAnalyzer.class);
    private static final String CONFIG_FILE = "ai";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String MODEL = "text-davinci-003";
    private static final int MAX_TOKENS = 1000;
    private static final double TEMPERATURE = 0.7;

    /**
     * Analyzes test results and generates insights
     * @param reportPath Path to the HTML report file
     * @param logPath Path to the test execution log file
     * @return AI-generated insights
     */
    public static String analyzeResults(String reportPath, String logPath) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(CONFIG_FILE);
            String apiKey = bundle.getString("openai.api.key");
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(30));

            // Extract relevant information from report and logs
            StringBuilder context = new StringBuilder();
            context.append(extractTestResults(reportPath))
                  .append("\n")
                  .append(extractErrorPatterns(logPath));

            // Prepare the analysis request
            String prompt = """
                Analyze the following test execution results and provide insights:
                
                Test Data:
                %s
                
                Focus on:
                1. Test execution patterns
                2. Common failure points
                3. Performance observations
                4. Recommendations for improvement""".formatted(context.toString());

            CompletionRequest request = CompletionRequest.builder()
                    .model(MODEL)
                    .prompt(prompt)
                    .maxTokens(MAX_TOKENS)
                    .temperature(TEMPERATURE)
                    .build();

            List<CompletionChoice> choices = service.createCompletion(request).getChoices();
            String analysis = choices.isEmpty() ? "No insights generated" : choices.get(0).getText();

            service.shutdownExecutor();
            return formatAnalysis(analysis);

        } catch (Exception e) {
            LOGGER.error("Failed to analyze test results: {}", e.getMessage(), e);
            return "AI Analysis failed: " + e.getMessage();
        }
    }

    private static String extractTestResults(String reportPath) throws Exception {
        StringBuilder results = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(reportPath)))) {
            String line;
            boolean collectingData = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("test-status") || line.contains("test-time")) {
                    collectingData = true;
                    results.append(line.trim()).append("\n");
                } else if (collectingData && line.contains("</div>")) {
                    collectingData = false;
                } else if (collectingData) {
                    results.append(line.trim()).append("\n");
                }
            }
        }
        return results.toString();
    }

    private static String extractErrorPatterns(String logPath) throws Exception {
        StringBuilder patterns = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(logPath)))) {
            String line;
            Pattern errorPattern = Pattern.compile("ERROR|FAIL|Exception|Timeout");
            
            while ((line = reader.readLine()) != null) {
                Matcher matcher = errorPattern.matcher(line);
                if (matcher.find()) {
                    patterns.append(line.trim()).append("\n");
                }
            }
        }
        return patterns.toString();
    }

    private static String formatAnalysis(String rawAnalysis) {
        return rawAnalysis.replaceAll("```", "")
                         .replaceAll("\n{3,}", "\n\n")
                         .trim();
    }
}