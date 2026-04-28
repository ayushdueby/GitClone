package com.GitClear.Git.ai;

import com.GitClear.Git.dag.CommitDAG;
import com.GitClear.Git.dto.TrajectoryResponse;
import com.GitClear.Git.helper.SerializeDiffs;
import com.GitClear.Git.model.*;
import com.GitClear.Git.refs.RefManager;
import com.GitClear.Git.service.AiService;
import com.GitClear.Git.service.DiffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConflictProphetService {
    @Autowired private BranchDivergenceAnalyzerService branchDivergenceAnalyzerService;
    @Autowired private AiService aiService;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Autowired private DiffService diffService;
    @Autowired private RefManager refManager;
    @Autowired private SerializeDiffs serializeDiffs;
    @Autowired private CommitDAG commitDAG;

    public ProphecyReport prophesy(String branch1, String branch2) {

        DivergenceReport report = branchDivergenceAnalyzerService.analyzeDivergence(branch1, branch2);

        List<FileCollisionRisk> risks = new ArrayList<>(report.getFileRisks().values());

        // filter low-risk a+b<2
        risks = risks.stream()
                .filter(r -> r.getTouchCountBranch1() + r.getTouchCountBranch2() >= 2)
                .toList();

        List<FileProphecy> results = new ArrayList<>();
        int batchSize = 3;

        for (int i = 0; i < risks.size(); i += batchSize) {

            List<FileCollisionRisk> batch =
                    risks.subList(i, Math.min(i + batchSize, risks.size()));
            StringBuilder sb = new StringBuilder();

            for (FileCollisionRisk risk : batch) {

                String file = risk.getFileName();
                sb.append("\n=== FILE: ").append(file).append(" ===\n");
                sb.append("Branch 1 changes:\n");
                for (String commit : risk.getBranch1Commits()) {
                    List<DiffLine> diffs = diffService.diffCommits(report.getLca(), commit, file);
                    sb.append(serializeDiffs.serialize(diffs)).append("\n");
                }
                sb.append("Branch 2 changes:\n");
                for (String commit : risk.getBranch2Commits()) {
                    List<DiffLine> diffs = diffService.diffCommits(report.getLca(), commit, file);
                    sb.append(serializeDiffs.serialize(diffs)).append("\n");
                }
            }
            String prompt = """
            You are analyzing multiple files modified across two Git branches.

            For EACH file, return a JSON object.

            Return STRICT JSON ARRAY only:

            [
              {
                "filename": "...",
                "conflictProbability": 0.0,
                "conflictType": "...",
                "reason": "...",
                "conflictingRegions": ["..."],
                "suggestion": "...",
                "safeToMerge": true
              }
            ]

            %s
            """.formatted(sb.toString());
            String response = aiService.generation(prompt);
            response = response.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            try {
                List<FileProphecy> batchResult = objectMapper.readValue(
                        response,
                        objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, FileProphecy.class)
                );
                results.addAll(batchResult);

            } catch (Exception e) {
                throw new RuntimeException("Batch parse failed: " + response, e);
            }
        }
        double avg = results.stream()
                .mapToDouble(FileProphecy::getConflictProbability)
                .average()
                .orElse(0.0);

        String recommendation =
                avg > 0.7 ? "DANGER" :
                        avg > 0.4 ? "COORDINATE" :
                                "MERGE NOW";
        return new ProphecyReport(
                branch1,
                branch2,
                report.getLca(),
                report.getCommitsBehind(),
                avg,
                results,
                recommendation
        );
    }
    public TrajectoryResponse getCollisionTrajectory(String branch1, String branch2) {

        String sha1 = refManager.getBranchSha(branch1);
        String sha2 = refManager.getBranchSha(branch2);

        List<String> history1 = commitDAG.getHistory(sha1);
        List<String> history2 = commitDAG.getHistory(sha2);

        List<Trajectory> trajectories = new ArrayList<>();
        int steps = Math.min(history1.size(), history2.size());
        for (int i = 1; i <= steps; i++) {
            String partialSha1 = history1.get(i - 1);
            String partialSha2 = history2.get(i - 1);
            // simulate branches at this point
            ProphecyReport report = prophesy(
                    partialSha1,
                    partialSha2
            );
            trajectories.add(new Trajectory(
                    partialSha1 + "|" + partialSha2,
                    report.getOverallConflictProbability()
            ));
        }
        // determine trend
        String trend = "CONTROLLED";

        if (trajectories.size() >= 2) {
            double first = trajectories.get(0).getProbability();
            double last = trajectories.get(trajectories.size() - 1).getProbability();

            if (last - first > 0.3) trend = "ACCELERATING";
            else if (last > first) trend = "INCREASING";
            else trend = "STABLE";
        }

        TrajectoryResponse response = new TrajectoryResponse();
        response.setTrajectories(trajectories);
        response.setTrend(trend);

        return response;
    }
}
