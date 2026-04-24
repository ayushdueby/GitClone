package com.GitClone.Git.model;

import lombok.Data;

import java.nio.charset.StandardCharsets;


import java.nio.charset.StandardCharsets;

@Data
public class Commit extends GitObject {

    private String treeSha;
    private String parentCommitSha; // null for first commit
    private String message;
    private String author;
    private long timestamp;

    public Commit(String treeSha, String parentCommitSha, String message, String author) {
        this.setType("commit");
        this.treeSha = treeSha;
        this.parentCommitSha = parentCommitSha;
        this.message = message;
        this.author = author;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();

        sb.append("tree ").append(treeSha).append("\n");

        if (parentCommitSha != null) {
            sb.append("parent ").append(parentCommitSha).append("\n");
        }

        sb.append("author ").append(author).append("\n");
        sb.append("timestamp ").append(timestamp).append("\n");
        sb.append("\n"); // separator
        sb.append(message).append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
