package com.GitClone.Git.service;

import com.GitClone.Git.model.Blob;
import com.GitClone.Git.model.Commit;
import com.GitClone.Git.refs.RefManager;
import com.GitClone.Git.store.ObjectStore;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitService {

    private RefManager refManager;
    private Map<String,String>indexStaging;
    private ObjectStore objectStore;
    private GitService(RefManager refManager,ObjectStore objectStore)
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
    public void gitAdd(String path,String content) throws DigestException, NoSuchAlgorithmException {
        Blob blob =new Blob(content.getBytes(StandardCharsets.UTF_8));
        String sha=objectStore.store(blob);
        indexStaging.put(path,sha);
    }
    public List<String> gitLog()
    {
        List<String>commitHistory=new ArrayList<>();
        String headSha=refManager.getHeadSha();
        while(headSha!=null)
        {
            Commit commit= (Commit) objectStore.get(headSha);
            commitHistory.add(headSha);
            headSha = commit.getParentCommitSha();
        }
        return commitHistory;
    }
}
