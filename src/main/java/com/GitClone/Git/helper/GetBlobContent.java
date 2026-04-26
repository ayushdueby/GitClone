package com.GitClone.Git.helper;

import com.GitClone.Git.model.Blob;
import com.GitClone.Git.store.ObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class GetBlobContent {
    @Autowired private ObjectStore objectStore;
    public String getContentFromBlob(String blobSha) {
        if (blobSha == null) return "";
        Blob blob = (Blob) objectStore.getGitObject(blobSha);
        return new String(blob.getContent(), StandardCharsets.UTF_8);
    }
}
