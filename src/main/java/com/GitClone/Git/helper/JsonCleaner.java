package com.GitClone.Git.helper;

import org.springframework.stereotype.Component;

@Component
public class JsonCleaner {

    public String cleanJson(String response) {
        if (response == null) return "";

        return response
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}
