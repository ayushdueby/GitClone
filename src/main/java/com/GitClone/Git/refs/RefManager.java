package com.GitClone.Git.refs;

import com.GitClone.Git.dag.CommitDAG;
import com.GitClone.Git.model.Commit;
import com.GitClone.Git.model.GitObject;
import com.GitClone.Git.model.Tree;
import com.GitClone.Git.store.ObjectStore;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
public class RefManager {
    private String headBranch;
    private Map<String,String>latestCommitByBranch; // branch->latest Committed sha
    private boolean isDetached = false;
    private String detachedHeadSha = null;
    @Autowired private CommitDAG commitDAG;
    @Autowired private ObjectStore objectStore;
    public RefManager()
    {
        this.latestCommitByBranch=new HashMap<>();
    }
    public String checkoutBranch(String checkOutBranch)
    {
        if(!latestCommitByBranch.containsKey(checkOutBranch))
        {
            throw new RuntimeException("Branch does not exists");
        }
        headBranch=checkOutBranch;
        isDetached = false;
        detachedHeadSha = null;
        //Restore the working directory -> meaning rebuild the index from that commit's tree
        //ye kya hai? (checkout by branch)
        //index?
        //tree restoration

        return headBranch;
    }
    public String checkoutCommit(String checkOutCommit)
    {
        isDetached = true;
        detachedHeadSha = checkOutCommit;
        //checkout commit ke andar sirf attach it to the sha
        return detachedHeadSha;
    }
    public void createBranch(String branchToAdd,String sha)
    {
        if(latestCommitByBranch.containsKey(branchToAdd))
        {
            throw new RuntimeException("Branch does already exists");
        }
        latestCommitByBranch.put(branchToAdd,sha);
    }
    public String getHeadSha()
    {
        if(isDetached)return detachedHeadSha;
        return latestCommitByBranch.get(headBranch);
    }
    //uska latest sha return hoga and we will update that into our key branch's value
    public void updateCurrentBranchAfterCommit(String newSha)
    {
        if(isDetached)return;
        latestCommitByBranch.put(headBranch,newSha);
    }
    public List<String>listBranches()
    {
        return new ArrayList<>(latestCommitByBranch.keySet());
    }
    public String getBranchSha(String branchName)
    {
        return latestCommitByBranch.getOrDefault(branchName,null);
    }
    public List<String> getLog()
    {
        return commitDAG.getHistory(latestCommitByBranch.get(headBranch));
    }
    public void addCommit(String commitSha,List<String>parents)
    {
        commitDAG.addCommit(commitSha,parents);
    }
    public String getBaseBranch(String sha1,String sha2)
    {
        return commitDAG.findLCA(sha1,sha2);
    }
    public void deleteBranch(String name, String currentBranch, String currentHeadSha) {
        if (!latestCommitByBranch.containsKey(name)) {
            throw new RuntimeException("BRANCH_NOT_FOUND");
        }

        if (name.equals(currentBranch)) {
            throw new RuntimeException("CANNOT_DELETE_CURRENT_BRANCH");
        }

        String branchSha = latestCommitByBranch.get(name);

        // Check if fully merged
        boolean isMerged = commitDAG.isAncestor(branchSha, currentHeadSha);
        if (!isMerged) {
            throw new RuntimeException("BRANCH_NOT_MERGED");
        }

        latestCommitByBranch.remove(name);
    }
}
