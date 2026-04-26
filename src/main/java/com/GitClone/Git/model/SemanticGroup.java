package com.GitClone.Git.model;

import lombok.Data;

import java.util.List;

@Data
public class SemanticGroup {
    private String explanation;
    private List<DiffLine> lines;
}
