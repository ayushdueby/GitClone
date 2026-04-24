package com.GitClone.Git.model;

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
