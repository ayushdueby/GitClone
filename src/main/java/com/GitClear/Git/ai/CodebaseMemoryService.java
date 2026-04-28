package com.GitClear.Git.ai;

import com.GitClear.Git.dag.CommitDAG;
import com.GitClear.Git.dto.CommitIntentResult;
import com.GitClear.Git.dto.IntentResult;
import com.GitClear.Git.dto.QueryResult;
import com.GitClear.Git.gitEnum.DiffType;
import com.GitClear.Git.model.Commit;
import com.GitClear.Git.model.DiffLine;
import com.GitClear.Git.model.Tree;
import com.GitClear.Git.refs.RefManager;
import com.GitClear.Git.service.AiService;
import com.GitClear.Git.service.DiffService;
import com.GitClear.Git.store.ObjectStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodebaseMemoryService {

    @Autowired
    private AiService aiService;
    @Autowired
    private CommitDAG commitDAG;
    @Autowired
    private DiffService diffService;
    @Autowired
    private ObjectStore objectStore;
    @Autowired
    private IntentPredictorService intentPredictorService;
    @Autowired
    private RefManager refManager;
    @Autowired
    private ObjectMapper objectMapper;

    public QueryResult query(String question, String branch) {

        String intentPrompt = """
                Classify this question about a Git repository into one of these intents:
                
                - WHO_CHANGED
                - WHAT_CHANGED
                - WHEN_CHANGED
                - WHY_CHANGED
                - MOST_ACTIVE
                - COMPARE
                - SUMMARY
                
                Question: %s
                
                Return JSON:
                {
                  "intent": "...",
                  "targetFile": "...",
                  "targetAuthor": "...",
                  "timeRange": 20
                }
                """.formatted(question);

        String intentResponse = aiService.generation(intentPrompt);

        intentResponse = intentResponse.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        IntentResult intent;
        try {
            intent = objectMapper.readValue(intentResponse, IntentResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Intent parse failed: " + intentResponse);
        }

        return handleIntent(intent, question, branch);
    }

    private QueryResult handleIntent(IntentResult intent, String question, String branch) {

        return switch (intent.getIntent()) {

            case "WHO_CHANGED" -> handleWhoChanged(intent, question, branch);
            case "WHAT_CHANGED" -> handleWhatChanged(intent, question, branch);
            case "WHY_CHANGED" -> handleWhyChanged(intent, question, branch);
            case "MOST_ACTIVE" -> handleMostActive(intent, question, branch);
            case "SUMMARY" -> handleSummary(intent, question, branch);

            default -> new QueryResult("Unsupported query", intent.getIntent(),
                    List.of(), List.of(), 0.5);
        };
    }

    private QueryResult handleSummary(IntentResult intent, String question, String branch) {

        String head = refManager.getBranchSha(branch);
        List<String> history = commitDAG.getHistory(head);

        String prompt = "Summarize codebase health based on commits:\n" + history;

        String answer = aiService.generation(prompt);

        return new QueryResult(answer, "SUMMARY", history, List.of(), 0.8);
    }
    private QueryResult handleMostActive(IntentResult intent, String question, String branch) {

        String head = refManager.getBranchSha(branch);
        List<String> history = commitDAG.getHistory(head);

        Map<String, Integer> fileCount = new HashMap<>();

        for (String sha : history) {
            Commit commit = (Commit) objectStore.getGitObject(sha);
            Tree tree = (Tree) objectStore.getGitObject(commit.getTreeSha());

            for (String file : tree.getEntries().keySet()) {
                fileCount.merge(file, 1, Integer::sum);
            }
        }

        String prompt = "Top active files:\n" + fileCount;

        String answer = aiService.generation(prompt);

        return new QueryResult(answer, "MOST_ACTIVE", history, new ArrayList<>(fileCount.keySet()), 0.85);
    }
    private QueryResult handleWhyChanged(IntentResult intent, String question, String branch) {

        String head = refManager.getBranchSha(branch);
        List<String> history = commitDAG.getHistory(head);

        StringBuilder reasoning = new StringBuilder();
        List<String> commitsUsed = new ArrayList<>();

        for (String sha : history) {

            CommitIntentResult result = intentPredictorService.predictIntent(sha);

            reasoning.append(result.getReasoning()).append("\n");

            commitsUsed.add(sha);

            if (commitsUsed.size() >= intent.getTimeRange()) break;
        }

        String prompt = "Summarize WHY changes happened:\n" + reasoning;

        String answer = aiService.generation(prompt);

        return new QueryResult(answer, "WHY_CHANGED", commitsUsed, List.of(), 0.9);
    }

    private QueryResult handleWhatChanged(IntentResult intent, String question, String branch) {

        String head = refManager.getBranchSha(branch);
        List<String> history = commitDAG.getHistory(head);

        StringBuilder diffData = new StringBuilder();
        List<String> commitsUsed = new ArrayList<>();

        for (String sha : history) {

            Commit commit = (Commit) objectStore.getGitObject(sha);
            List<String> parents = commit.getParentCommitSha();

            for (String parent : parents) {
                List<DiffLine> diffs = diffService.diffCommits(parent, sha, intent.getTargetFile());
                for(DiffLine diff:diffs)
                {
                    if(diff.getType()!= DiffType.UNCHANGED)
                        diffData.append(diff.getDiffContent()).append("\n");
                }
            }

            commitsUsed.add(sha);

            if (commitsUsed.size() >= intent.getTimeRange()) break;
        }

        String prompt = "Summarize changes:\n" + diffData;

        String answer = aiService.generation(prompt);

        return new QueryResult(answer, "WHAT_CHANGED", commitsUsed,
                List.of(intent.getTargetFile()), 0.9);
    }

    private QueryResult handleWhoChanged(IntentResult intent, String question, String branch) {

        String head = refManager.getBranchSha(branch);
        List<String> history = commitDAG.getHistory(head);

        Map<String, Integer> authorCount = new HashMap<>();
        List<String> commitsUsed = new ArrayList<>();

        for (String sha : history) {

            Commit commit = (Commit) objectStore.getGitObject(sha);

            if (intent.getTargetFile() != null) {
                Tree tree = (Tree) objectStore.getGitObject(commit.getTreeSha());
                if (!tree.getEntries().containsKey(intent.getTargetFile())) continue;
            }

            authorCount.merge(commit.getAuthor(), 1, Integer::sum);
            commitsUsed.add(sha);

            if (commitsUsed.size() >= intent.getTimeRange()) break;
        }

        String prompt = "Summarize who worked on this:\n" + authorCount;

        String answer = aiService.generation(prompt);

        return new QueryResult(
                answer,
                "WHO_CHANGED",
                commitsUsed,
                List.of(intent.getTargetFile()),
                0.9
        );
    }
}
