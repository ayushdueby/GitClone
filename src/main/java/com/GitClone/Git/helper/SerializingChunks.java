package com.GitClone.Git.helper;

import com.GitClone.Git.model.MergeChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SerializingChunks {
    public String serializeChunks(List<MergeChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (MergeChunk chunk : chunks) {
            for (String line : chunk.getMerges()) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
