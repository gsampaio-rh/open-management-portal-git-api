package com.rht_labs.omp.models;

import java.util.HashMap;
import java.util.Map;

public class EditFileInGitRequest {
    public String branch = "master";
    public String author_email = "auto@changeme.com";
    public String encoding = "base64";
    public String author_name = "Jenkins";
    public String file_path;
    public String content;
    public String commit_message = "🔥 Automated commit from terribly named OMP 🔥";
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}