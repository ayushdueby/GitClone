package com.GitClone.Git.store;

import com.GitClone.Git.model.GitObject;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Data
public class ObjectStore {

    private Map<String,GitObject>gitObjectToShaStore;
    ObjectStore()
    {
        gitObjectToShaStore.clear();
    }
    public String computeSha(byte[] content) throws DigestException, NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = md.digest(content);

            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public String store(GitObject gitObject) throws DigestException, NoSuchAlgorithmException {
        byte[] gitObjectdata=gitObject.serialize();
        String sha=computeSha(gitObjectdata);
        gitObject.setSha(sha);
        gitObjectToShaStore.putIfAbsent(sha,gitObject);
        return sha;
    }
    public GitObject get(String sha) {
        return gitObjectToShaStore.get(sha);
    }
}
