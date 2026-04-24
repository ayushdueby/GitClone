package com.GitClone.Git.refs;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RefManager {
    private String headBranch;
    private Map<String,String>latestCommitByBranch;

    public RefManager()
    {
        this.latestCommitByBranch=new HashMap<>();
    }
    public String checkOut(String checkOutBranch)
    {
        if(!latestCommitByBranch.containsKey(checkOutBranch))
        {
            throw new RuntimeException("Branch does not exists");
        }
        headBranch=checkOutBranch;
        return headBranch;
    }
    public void createBranch(String branchToAdd,String sha)
    {
        latestCommitByBranch.put(branchToAdd,sha);
    }
    public String getHeadSha()
    {
        return latestCommitByBranch.get(headBranch);
    }
    //uska latest sha return hoga and we will update that into our key branch's value
    public void updateCurrentBranchAfterCommit(String newSha)
    {
        latestCommitByBranch.put(headBranch,newSha);
    }
}
