package com.GitClone.Git.model;

import lombok.Data;

@Data
public class Blob extends GitObject{

    public byte[] content;
    public Blob(byte[] content)
    {
        this.setType("blob");
        this.content=content;
    }

    @Override
    public byte[] serialize() {
        return content;
    }

}
