package com.GitClear.Git.controller;

import com.GitClear.Git.ai.CodebaseMemoryService;
import com.GitClear.Git.ai.ConflictProphetService;
import com.GitClear.Git.ai.IntentPredictorService;
import com.GitClear.Git.ai.SemanticDiffService;
import com.GitClear.Git.dag.CommitDAG;
import com.GitClear.Git.dto.*;
import com.GitClear.Git.model.DiffLine;
import com.GitClear.Git.model.ProphecyReport;
import com.GitClear.Git.service.DiffService;
import com.GitClear.Git.service.GitService;
import com.GitClear.Git.service.MergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/git")
public class GitController {

    @Autowired public GitService gitService;
    @Autowired public DiffService diffService;
    @Autowired public MergeService mergeService;
    @Autowired public SemanticDiffService semanticDiffService;
    @Autowired public IntentPredictorService intentPredictorService;
    @Autowired public CommitDAG commitDAG;
    @Autowired public ConflictProphetService conflictProphetService;
    @Autowired public CodebaseMemoryService codebaseMemoryService;

    @PostMapping("/init")
    public void gitInit()
    {
        gitService.gitInit();
    }
    @PostMapping("/add")
    public void gitAdd(@RequestParam String path, @RequestBody String body) throws DigestException, NoSuchAlgorithmException {
        gitService.gitAdd(path,body);
    }
    @PostMapping("/branch")
    public void gitCreateBranch(@RequestParam String name)
    {
        gitService.gitCreateBranch(name);
    }
    @PostMapping("/checkout")
    public void gitCheckoutBranch(@RequestParam String target)
    {
        gitService.gitCheckout(target);
    }
    @PostMapping("/merge")
    public void gitMerge(@RequestParam String branch) throws DigestException, NoSuchAlgorithmException {
        mergeService.canMerge(branch,true);
    }
    @PostMapping("/commit")
    public ResponseEntity<String> gitCommit(
            @RequestBody CommitRequest request
    ) throws Exception {
        return ResponseEntity.ok(
                gitService.gitCommit(request.getMessage(), request.getAuthor())
        );
    }


    @GetMapping("/log")
    public List<String> gitLog()
    {
        return gitService.gitLog();
    }
    @GetMapping("/branch")
    public List<String>gitListBranch()
    {
        return gitService.gitListBranch();
    }
    @GetMapping("/diff")
    public List<DiffLine> getDiff(@RequestParam String sha1,@RequestParam String sha2,@RequestParam String file)
    {
        return diffService.diffCommits(sha1,sha2,file);
    }
    @GetMapping("/diff/working")
    public List<DiffLine> getDiffHeadVsCurr(@RequestParam String file,@RequestParam String newContent)
    {
        return diffService.diffWorkingVsHead(file,newContent);
    }
    @GetMapping("/merge/preview")
    public String gitCheckMergeConflicts(@RequestParam String branch) throws DigestException, NoSuchAlgorithmException {
        boolean canMerge = mergeService.canMerge(branch, false);
        if(!canMerge)
            return mergeService.mergeConflictStore.toString();
        else
            return "No conflicts";
    }

    @DeleteMapping("/branch")
    public ResponseEntity<?> deleteBranch(@RequestParam String name) {
        return ResponseEntity.ok(gitService.deleteBranch(name));
    }
    @GetMapping("/log/graph")
    public ResponseEntity<?> getGraph() {
        return ResponseEntity.ok(gitService.getGraphLog());
    }
    @GetMapping("/ai/diff")
    public ResponseEntity<SemanticDiffResult> getAiDiffSuggestions(
            @RequestParam String sha1,
            @RequestParam String sha2,
            @RequestParam("file") String file
    ) {
        return ResponseEntity.ok(
                semanticDiffService.semanticDiff(sha1, sha2, file)
        );
    }
    @GetMapping("/ai/intent")
    public ResponseEntity<CommitIntentResult> getAiCommitIntent(
            @RequestParam String sha1
    ) {
        return ResponseEntity.ok(
                intentPredictorService.predictIntent(sha1)
        );
    }
    @GetMapping("/ai/intent/batch")
    public ResponseEntity<CommitIntentResult> getAiCommitIntentBatch(
            @RequestParam String branch,
            @RequestParam Integer limit
    ) {
        List<String>getLastTencommit=commitDAG.getHistory(gitService.getBranchsha(branch)).stream()
                .limit(limit)
                .toList();
        return ResponseEntity.ok(
                intentPredictorService.predictIntentBatch(getLastTencommit)
        );
    }
    @GetMapping("/ai/prophet")
    public ResponseEntity<ProphecyReport> getAiProphet(
            @RequestParam String branch1,
            @RequestParam String branch2
    ) {
        return ResponseEntity.ok(
                conflictProphetService.prophesy(branch1,branch2)
        );
    }
    @GetMapping("/ai/prophet/trajectory")
    public ResponseEntity<TrajectoryResponse> getAiProphetTrajectory(
            @RequestParam String branch1,
            @RequestParam String branch2
    ) {
        return ResponseEntity.ok(
                conflictProphetService.getCollisionTrajectory(branch1,branch2)
        );
    }
    @GetMapping("/ai/prophet/quick")
    public double getAiProphetTrajectory2(
            @RequestParam String branch1,
            @RequestParam String branch2
    ) {
        return conflictProphetService.prophesy(branch1,branch2).getOverallConflictProbability();
    }
    @PostMapping("/ai/query")
    public ResponseEntity<QueryResult> query(
            @RequestParam String branch,
            @RequestBody String question
    ) {
        return ResponseEntity.ok(
                codebaseMemoryService.query(question, branch)
        );
    }


}
