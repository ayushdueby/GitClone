package com.GitClear.Git.dto;

import lombok.Data;

@Data
public class IntentResult {
    private String intent;
    private String targetFile;
    private String targetAuthor;
    private Integer timeRange;
}
