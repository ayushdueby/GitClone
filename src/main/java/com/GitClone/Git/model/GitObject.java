package com.GitClone.Git.model;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class GitObject {
    private String sha;
    private String type;
    public abstract byte[] serialize();
}
