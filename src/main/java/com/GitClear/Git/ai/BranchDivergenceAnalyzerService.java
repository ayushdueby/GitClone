package com.GitClear.Git.ai;

import com.GitClear.Git.dag.CommitDAG;
import com.GitClear.Git.diff.DiffEngine;
import com.GitClear.Git.model.Commit;
import com.GitClear.Git.model.DivergenceReport;
import com.GitClear.Git.model.FileCollisionRisk;
import com.GitClear.Git.model.Tree;
import com.GitClear.Git.refs.RefManager;
import com.GitClear.Git.store.ObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BranchDivergenceAnalyzerService {
    @Autowired private RefManager refManager;
    @Autowired private CommitDAG commitDAG;
    @Autowired private DiffEngine diffEngine;
    @Autowired private ObjectStore objectStore;
    public DivergenceReport analyzeDivergence(String branch1, String branch2){
        String sha1=refManager.getBranchSha(branch1);
        String sha2=refManager.getBranchSha(branch2);

        String lcaCommit=commitDAG.findLCA(sha1,sha2);
        Tree lcaTree=(Tree)objectStore.getGitObject(lcaCommit);

        List<String>historySha1=commitDAG.getHistoryTillGivenCommit(sha1,lcaCommit);
        List<String>historySha2=commitDAG.getHistoryTillGivenCommit(sha2,lcaCommit);

        Map<String, List<String>>branch1TouchedFiles=new HashMap<>();
        Map<String, List<String>> branch2TouchedFiles=new HashMap<>();

        for(String commitSha:historySha1)
        {
            Commit commit=(Commit) objectStore.getGitObject(commitSha);
            Tree tree=(Tree)objectStore.getGitObject(commit.getTreeSha());
            for(String fileName:tree.getEntries().keySet())
            {
                if(tree.getBlobSha(fileName).equals(lcaTree.getBlobSha(fileName)))
                    continue;
                else
                {
                    branch1TouchedFiles
                            .computeIfAbsent(fileName, k -> new ArrayList<>())
                            .add(commitSha);
                }
            }
        }
        for(String commitSha:historySha2)
        {
            Commit commit=(Commit) objectStore.getGitObject(commitSha);
            Tree tree=(Tree)objectStore.getGitObject(commit.getTreeSha());
            for(String fileName:tree.getEntries().keySet())
            {
                if(tree.getBlobSha(fileName).equals(lcaTree.getBlobSha(fileName)))
                    continue;
                else
                {
                    branch2TouchedFiles
                            .computeIfAbsent(fileName, k -> new ArrayList<>())
                            .add(commitSha);
                }
            }
        }
        Set<String>intersectingFiles=new HashSet<>(branch1TouchedFiles.keySet());
        intersectingFiles.retainAll(branch2TouchedFiles.keySet());

        int commitsBehind=commitDAG.findLCASize(sha1,sha2);
        //lca=lcaCommit
        Map<String, FileCollisionRisk>filRisks=new HashMap<>();

        for(String fileName:intersectingFiles)
        {
            FileCollisionRisk fileCollisionRisk=new FileCollisionRisk();
            fileCollisionRisk.setFileName(fileName);

            fileCollisionRisk.setBranch1Commits(branch1TouchedFiles.get(fileName));
            fileCollisionRisk.setBranch2Commits(branch2TouchedFiles.get(fileName));

            fileCollisionRisk.setTouchCountBranch1(branch1TouchedFiles.get(fileName).size());
            fileCollisionRisk.setTouchCountBranch2(branch2TouchedFiles.get(fileName).size());

            filRisks.putIfAbsent(fileName,fileCollisionRisk);
        }
        return new DivergenceReport(
                lcaCommit,
                commitsBehind,
                filRisks
        );
    }
}
