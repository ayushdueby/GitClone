package com.GitClone.Git.model;

import com.GitClone.Git.gitEnum.MergeEnum;
import lombok.Data;

import java.util.List;

@Data
public class MergeChunk {
    private MergeEnum type;
    private List<String>merges;

    public MergeChunk(MergeEnum mergeEnum,List<String>merges)
    {
        this.merges=merges;
        this.type=mergeEnum;
    }
}
