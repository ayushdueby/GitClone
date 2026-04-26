package com.GitClone.Git.dto;

import com.GitClone.Git.model.SemanticGroup;
import lombok.Data;

import java.util.List;

@Data
public class SemanticDiffResult {
    private String summary;
    private String type;
    private List<SemanticGroup> semanticGroups;
    private List<String> suggestions;
    private String riskLevel;
}
