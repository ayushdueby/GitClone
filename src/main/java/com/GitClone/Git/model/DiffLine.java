package com.GitClone.Git.model;

import com.GitClone.Git.diffEnum.DiffType;
import lombok.Data;

@Data
public class DiffLine {
    private String diffContent;
    private DiffType type;

    public DiffLine(String diffContent, DiffType type)
    {
        this.diffContent=diffContent;
        this.type=type;
    }

}
