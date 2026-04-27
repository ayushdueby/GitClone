package com.GitClear.Git.dag;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CommitDAG {

    private final Map<String,List<String>>childShaToParentSha; // childCommitSha->parentCommitSha

    public CommitDAG()
    {
        this.childShaToParentSha=new HashMap<>();
    }
    public void addCommit(String sha,List<String> parentSha)
    {
        childShaToParentSha.put(sha,new ArrayList<>(parentSha));
    }
    public List<String> getParents(String childSha)
    {
        return childShaToParentSha.getOrDefault(childSha,Collections.emptyList());
    }
    public List<String> getHistory(String startSha) {

        List<String> history = new ArrayList<>();
        if(startSha==null)return history;

        Set<String> visited = new HashSet<>();

        Stack<String> stack = new Stack<>();
        stack.push(startSha);

        while (!stack.isEmpty()) {
            String curr = stack.pop();

            if (curr == null || visited.contains(curr)) continue;

            visited.add(curr);
            history.add(curr);

            for (String parent : getParents(curr)) {
                if(parent!=null)stack.push(parent);
            }
        }

        return history;
    }
    public List<String> getHistoryTillGivenCommit(String startSha,String endSha) {

        List<String> history = new ArrayList<>();
        if(startSha==null)return history;

        Set<String> visited = new HashSet<>();

        Stack<String> stack = new Stack<>();
        stack.push(startSha);

        while (!stack.isEmpty()) {
            String curr = stack.pop();

            if (curr == null || visited.contains(curr) || curr.equals(endSha)) continue;

            visited.add(curr);
            history.add(curr);

            for (String parent : getParents(curr)) {
                if(parent!=null)stack.push(parent);
            }
        }

        return history;
    }
    public String findLCA(String sha1, String sha2) {

        if(sha1==null || sha2==null)return null;

        Set<String>historySha1=new HashSet<>(getHistory(sha1));
        Set<String> vis = new HashSet<>();

        Queue<String>queue=new LinkedList<>();
        queue.add(sha2);

        while(!queue.isEmpty())
        {
            String front=queue.poll();

            if(front==null || vis.contains(front))continue;
            if(historySha1.contains(front))return front;
            vis.add(front);

            for(String parent:getParents(front))
            {
                queue.add(parent);
            }
        }
        return null;

    }
    public boolean isAncestor(String potentialAncestor, String ofSha)
    {
        return getHistory(ofSha).contains(potentialAncestor);
    }

}
