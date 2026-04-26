package com.GitClone.Git.service;

import com.GitClone.Git.dto.GraphResponse;
import com.GitClone.Git.model.*;
import com.GitClone.Git.refs.RefManager;
import com.GitClone.Git.store.ObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class GitService {

    private RefManager refManager;
    private Map<String,String>indexStaging; //file_path->sha
    @Autowired private ObjectStore objectStore;
    public GitService(RefManager refManager,ObjectStore objectStore)
    {
        this.objectStore=objectStore;
        this.indexStaging=new HashMap<>();
        this.refManager=refManager;
    }
    public void gitInit() {
        if (refManager.getLatestCommitByBranch().containsKey("main")) {
            throw new RuntimeException("Repository already initialized");
        }
        refManager.createBranch("main", null);
        refManager.setHeadBranch("main");
    }

    public String getHeadSha()
    {
        return refManager.getHeadSha();
    }

    public void gitAdd(String path,String content) throws DigestException, NoSuchAlgorithmException {
        Blob blob =new Blob(content.getBytes(StandardCharsets.UTF_8));
        String sha=objectStore.store(blob);
        indexStaging.put(path,sha);
    }
    public List<String> gitLog()
    {
        return refManager.getLog();
    }
    public void gitCheckout(String input) {

        String sha;

        if (refManager.getLatestCommitByBranch().containsKey(input)) {

            // branch checkout
            refManager.checkoutBranch(input);
            sha = refManager.getHeadSha();

        } else {
            GitObject obj = objectStore.getGitObject(input);

            if (obj == null || !(obj instanceof Commit)) {
                throw new RuntimeException("Invalid branch or commit SHA");
            }

            refManager.checkoutCommit(input);
            sha = input;
        }

        if (sha == null) {
            indexStaging.clear();
            return;
        }

        Commit commit = (Commit) objectStore.getGitObject(sha);

        if (commit.getTreeSha() == null) {
            indexStaging.clear();
            return;
        }

        Tree tree = (Tree) objectStore.getGitObject(commit.getTreeSha());

        if (tree == null) {
            throw new RuntimeException("Tree not found");
        }

        indexStaging.clear();
        indexStaging.putAll(tree.getEntries());
    }
    public void gitCreateBranch(String branchName)
    {
        refManager.createBranch(branchName,null);
    }
    public List<String>gitListBranch()
    {
        return refManager.listBranches();
    }
    public String deleteBranch(String name) {
        String currentBranch = refManager.getHeadBranch(); // FIX
        String currentHeadSha = refManager.getHeadSha();

        refManager.deleteBranch(name, currentBranch, currentHeadSha);

        return "Branch deleted successfully: " + name;
    }
    public GraphResponse getGraphLog() {
        Set<String> visited = new HashSet<>();

        // Start BFS from all branch heads
        Map<String, String> branches = refManager.getLatestCommitByBranch();
        Queue<String> queue = new LinkedList<>(branches.values());

        List<GraphNode> nodes = new ArrayList<>();

        while (!queue.isEmpty()) {
            String sha = queue.poll();
            if (sha == null || visited.contains(sha)) continue;

            visited.add(sha);

            Commit commit = (Commit) objectStore.getGitObject(sha);

            nodes.add(new GraphNode(
                    sha,
                    commit.getMessage(),
                    commit.getAuthor(),
                    commit.getTimestamp(),
                    commit.getParentCommitSha()
            ));

            queue.addAll(commit.getParentCommitSha());
        }

        return new GraphResponse(nodes, branches);
    }
}
