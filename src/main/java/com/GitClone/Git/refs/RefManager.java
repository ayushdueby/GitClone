package com.GitClone.Git.refs;

import com.GitClone.Git.model.Commit;
import com.GitClone.Git.model.GitObject;
import com.GitClone.Git.model.Tree;
import com.GitClone.Git.store.ObjectStore;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RefManager {
    private String headBranch;
    private Map<String,String>latestCommitByBranch; // branch->latest Committed sha
    private ObjectStore objectStore;
    private boolean isDetached = false;
    private String detachedHeadSha = null;

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
    //curr branch delete nai kar sakte
    public void deleteBranch(String branchName)
    {
        if (!latestCommitByBranch.containsKey(branchName)) {
            throw new RuntimeException("Branch does not exist");
        }
        if (branchName.equals(headBranch)) {
            throw new RuntimeException("Cannot delete current branch");
        }
        latestCommitByBranch.remove(branchName);
    }
    public List<String>listBranches()
    {
        return new ArrayList<>(latestCommitByBranch.keySet());
    }
    public String getBranchSha(String branchName)
    {
        return latestCommitByBranch.getOrDefault(branchName,null);
    }
}
