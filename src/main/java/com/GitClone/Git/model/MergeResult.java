package com.GitClone.Git.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MergeResult {
    private boolean hasConflicts;
    private List<MergeChunk> chunks;

    public MergeResult(boolean hasConflicts, List<MergeChunk>chunks)
    {
        this.chunks=chunks;
        this.hasConflicts=hasConflicts;
    }

}
