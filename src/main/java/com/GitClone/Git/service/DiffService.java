package com.GitClone.Git.service;

import com.GitClone.Git.diff.DiffEngine;
import com.GitClone.Git.model.Blob;
import com.GitClone.Git.model.Commit;
import com.GitClone.Git.model.DiffLine;
import com.GitClone.Git.model.Tree;
import com.GitClone.Git.store.ObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DiffService {

    private final ObjectStore objectStore;
    private final GitService gitService;
    private final DiffEngine diffEngine;

    public DiffService(ObjectStore objectStore, GitService gitService, DiffEngine diffEngine) {
        this.objectStore = objectStore;
        this.gitService = gitService;
        this.diffEngine = diffEngine;
    }

    public List<DiffLine> diffCommits(String sha1, String sha2, String filePath)
    {
        Commit commit1 = (Commit) objectStore.getGitObject(sha1);
        Commit commit2 = (Commit) objectStore.getGitObject(sha2);

        Tree tree1 = (Tree) objectStore.getGitObject(commit1.getTreeSha());
        Tree tree2 = (Tree) objectStore.getGitObject(commit2.getTreeSha());


        String blobSha1 = tree1.getBlobSha(filePath);
        String blobSha2 = tree2.getBlobSha(filePath);

        String str1 = blobSha1 == null ? "" :
                new String(((Blob) objectStore.getGitObject(blobSha1)).serialize(), StandardCharsets.UTF_8);

        String str2 = blobSha2 == null ? "" :
                new String(((Blob) objectStore.getGitObject(blobSha2)).serialize(), StandardCharsets.UTF_8);

        return diffEngine.diff(str1,str2);
    }
    public List<DiffLine> diffWorkingVsHead(String filePath,String newContent)
    {
        String headSha= gitService.getHeadSha();
        Commit commit= (Commit) objectStore.getGitObject(headSha);

        Tree tree=(Tree) objectStore.getGitObject(commit.getTreeSha());

        String blobSha = tree.getBlobSha(filePath);

        String blobContent = blobSha == null ? "" :
                new String(((Blob) objectStore.getGitObject(blobSha)).serialize(), StandardCharsets.UTF_8);

        return diffEngine.diff(blobContent,newContent);
    }

}
