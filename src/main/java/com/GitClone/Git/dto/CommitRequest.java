package com.GitClone.Git.dto;

import lombok.Data;

@Data
public class CommitRequest {
    private String message;
    private String author;
}
