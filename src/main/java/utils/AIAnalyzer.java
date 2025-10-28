package utils;

import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AIAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIAnalyzer.class);
    private static final Properties properties = new Properties();
    private static final ThreadLocal<OpenAiService> openAiServiceThreadLocal = new ThreadLocal<>();
    private static final String MODEL = "text-davinci-003";
    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.7;

    static {
        try {
            properties.load(AIAnalyzer.class.getClassLoader().getResourceAsStream("global.properties"));
        } catch (IOException e) {
            LOGGER.error("Failed to load properties", e);
            throw new RuntimeException("Failed to initialize properties", e);
        }
    }

    private static synchronized OpenAiService getOpenAiService() {
        OpenAiService service = openAiServiceThreadLocal.get();
        if (service == null) {
            String apiKey = properties.getProperty("openai.api.key");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOGGER.error("OpenAI API key not found in global.properties");
                throw new RuntimeException("OpenAI API key not found in global.properties");
            }
            service = new OpenAiService(apiKey, Duration.ofSeconds(30));
            openAiServiceThreadLocal.set(service);
        }
        return service;
    }

    public static String analyzeTestResult(ITestResult result) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Test Method: ").append(result.getMethod().getMethodName()).append("\n");
            context.append("Status: ").append(getTestStatus(result)).append("\n");
            
            Throwable throwable = result.getThrowable();
            if (throwable != null) {
                String errorMessage = throwable.getMessage();
                context.append("Error: ").append(errorMessage != null ? errorMessage : "No error message").append("\n");
                context.append("Stack Trace:\n").append(getStackTrace(throwable)).append("\n");
            }

            Object[] parameters = result.getParameters();
            if (parameters != null && parameters.length > 0) {
                context.append("Parameters:\n");
                for (Object param : parameters) {
                    context.append("- ").append(param).append("\n");
                }
            }

            return generateInsights(context.toString());
        } catch (Exception e) {
            LOGGER.error("Error analyzing test result", e);
            return "Error analyzing test result: " + e.getMessage();
        }
    }

    public static String analyzeTestSuite(List<ITestResult> results) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Test Suite Summary:\n");
            
            int passed = 0, failed = 0, skipped = 0;
            List<String> failures = new ArrayList<>();
            
            for (ITestResult result : results) {
                switch (result.getStatus()) {
                    case ITestResult.SUCCESS -> passed++;
                    case ITestResult.FAILURE -> {
                        failed++;
                        Throwable throwable = result.getThrowable();
                        String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
                        failures.add(result.getMethod().getMethodName() + ": " + errorMessage);
                    }
                    case ITestResult.SKIP -> skipped++;
                }
            }
            
            context.append("Total Tests: ").append(results.size()).append("\n");
            context.append("Passed: ").append(passed).append("\n");
            context.append("Failed: ").append(failed).append("\n");
            context.append("Skipped: ").append(skipped).append("\n\n");
            
            if (!failures.isEmpty()) {
                context.append("Failures:\n");
                failures.forEach(f -> context.append("- ").append(f).append("\n"));
            }

            return generateInsights(context.toString());
        } catch (Exception e) {
            LOGGER.error("Error analyzing test suite", e);
            return "Error analyzing test suite: " + e.getMessage();
        }
    }

    private static String generateInsights(String context) {
        try {
            String prompt = """
                Please analyze this test execution data and provide insights:
                
                %s
                
                Provide a concise analysis focusing on:
                1. Key observations
                2. Potential issues or patterns
                3. Recommendations for improvement""".formatted(context);

            CompletionRequest request = CompletionRequest.builder()
                    .model(MODEL)
                    .prompt(prompt)
                    .maxTokens(MAX_TOKENS)
                    .temperature(TEMPERATURE)
                    .build();

            List<CompletionChoice> choices = getOpenAiService().createCompletion(request).getChoices();
            return choices.isEmpty() ? "No insights generated" : choices.get(0).getText();
        } catch (Exception e) {
            LOGGER.error("Error generating insights", e);
            return "Error generating insights: " + e.getMessage();
        }
    }

    private static String getTestStatus(ITestResult result) {
        return switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP -> "SKIPPED";
            default -> "UNKNOWN";
        };
    }

    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (element.getClassName().startsWith("com.")) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public static void cleanup() {
        OpenAiService service = openAiServiceThreadLocal.get();
        if (service != null) {
            try {
                service.shutdownExecutor();
            } finally {
                openAiServiceThreadLocal.remove();
            }
        }
    }
}