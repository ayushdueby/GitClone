package com.GitClone.Git.model;

import java.util.List;

public class GraphNode {
    public String sha;
    public String message;
    public String author;
    public long timestamp;
    public List<String> parents;

    public GraphNode(String sha, String message, String author, long timestamp, List<String> parents) {
        this.sha = sha;
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
        this.parents = parents;
    }
}