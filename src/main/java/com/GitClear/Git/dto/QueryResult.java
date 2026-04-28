package com.GitClear.Git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryResult {
    private String answer;
    private String intent;
    private List<String> supportingCommits;
    private List<String> supportingFiles;
    private double confidence;
}
