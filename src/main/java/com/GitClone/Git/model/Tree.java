package com.GitClone.Git.model;

import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Data
public class Tree extends GitObject {

    private Map<String, String> entries = new HashMap<>();
    // filename → SHA

    public Tree() {
        this.setType("tree");
    }

    public void addEntry(String name, String sha) {
        entries.put(name, sha);
    }

    @Override
    public byte[] serialize() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String line = entry.getKey() + ":" + entry.getValue() + "\n";
                out.write(line.getBytes());
            }

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
