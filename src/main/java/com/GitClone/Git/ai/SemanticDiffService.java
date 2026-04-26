package com.GitClone.Git.ai;

import com.GitClone.Git.dto.SemanticDiffResult;
import com.GitClone.Git.gitEnum.DiffType;
import com.GitClone.Git.helper.JsonCleaner;
import com.GitClone.Git.model.DiffLine;
import com.GitClone.Git.model.SemanticGroup;
import com.GitClone.Git.service.DiffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Service
public class SemanticDiffService {
    @Autowired DiffService diffService;
    @Autowired JsonCleaner jsonCleaner;

    public SemanticDiffResult semanticDiff(String sha1, String sha2, String filePath)
    {
        List<DiffLine> diffLineList=diffService.diffCommits(sha1,sha2,filePath);
        StringBuilder sb=new StringBuilder();
        for(DiffLine diffLine:diffLineList)
        {
            if(diffLine.getType()== DiffType.UNCHANGED)
                continue;
            sb.append(diffLine.getType()).append(": ").append(diffLine.getDiffContent()).append("\n");
        }
        String prompt="You are a code review expert. Analyze this diff and return JSON with:\n" +
                "1. \"summary\" - one line what changed\n" +
                "2. \"type\" - REFACTOR / BUGFIX / FEATURE / RENAME / CLEANUP\n" +
                "3. \"semanticGroups\" - group related changes together with explanation\n" +
                "4. \"suggestions\" - list of improvement suggestions\n" +
                "5. \"riskLevel\" - LOW / MEDIUM / HIGH with reason\n" +
                "\n" +
                "Diff:\n```diff\n" + sb + "\n```\n" +
                "Return only valid JSON, nothing else.";
        //feed this prompt to geminai API:
        String aiResult=generation(prompt);
        return parseResponse(aiResult);
    }
    public SemanticDiffResult parseResponse(String aiResponse) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String clean = jsonCleaner.cleanJson(aiResponse);

            // Extract Gemini "text"
            if (clean.contains("\"text\":")) {
                int start = clean.indexOf("\"text\":") + 8;
                int end = clean.lastIndexOf("\"");
                clean = clean.substring(start, end);
            }

            return mapper.readValue(clean, SemanticDiffResult.class);

        } catch (Exception e) {
            e.printStackTrace();
            return fallbackResult(aiResponse);
        }
    }
    private SemanticDiffResult fallbackResult(String raw) {
        SemanticDiffResult res = new SemanticDiffResult();

        res.setSummary("Failed to parse AI response");
        res.setType("UNKNOWN");
        res.setSuggestions(List.of("Check raw response"));

        return res;
    }
    public String generation(String prompt) {
        try {

            String apiKey=System.getProperty("GEMINI_API_KEY");
            if (apiKey == null) {
                throw new RuntimeException("GEMINI_API_KEY_NOT_SET");
            }
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=" + apiKey;
            String escapedPrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            String body = "{ \"contents\": [{ \"role\": \"user\", \"parts\": [{\"text\": \""
                    + escapedPrompt + "\"}]}]}";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            InputStream stream = conn.getResponseCode() >= 400
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String response = br.lines().reduce("", (a, b) -> a + b);

            System.out.println("PROMPT:\n" + prompt);
            System.out.println("RESPONSE:\n" + response);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

}
