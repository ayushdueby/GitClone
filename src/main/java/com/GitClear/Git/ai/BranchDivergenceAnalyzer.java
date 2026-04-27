package com.GitClear.Git.ai;

import com.GitClear.Git.dag.CommitDAG;
import com.GitClear.Git.diff.DiffEngine;
import com.GitClear.Git.model.Commit;
import com.GitClear.Git.model.Tree;
import com.GitClear.Git.refs.RefManager;
import com.GitClear.Git.store.ObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BranchDivergenceAnalyzer {
    @Autowired private RefManager refManager;
    @Autowired private CommitDAG commitDAG;
    @Autowired private DiffEngine diffEngine;
    @Autowired private ObjectStore objectStore;
    public void analyzeDivergence(String branch1, String branch2){
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
                    if(branch1TouchedFiles.getOrDefault(fileName,null)==null)
                        branch1TouchedFiles.put(fileName,new ArrayList<>());
                    else
                        branch1TouchedFiles.get(fileName).add(commitSha);
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
                    if(branch1TouchedFiles.getOrDefault(fileName,null)==null)
                        branch1TouchedFiles.put(fileName,new ArrayList<>());
                    else
                        branch1TouchedFiles.get(fileName).add(commitSha);
                }
            }
        }
    }
}
