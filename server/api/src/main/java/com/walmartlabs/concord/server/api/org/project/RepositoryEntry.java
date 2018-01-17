package com.walmartlabs.concord.server.api.org.project;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 Wal-Mart Store, Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.walmartlabs.concord.common.validation.ConcordKey;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepositoryEntry implements Serializable {

    private final UUID id;

    private final UUID projectId;

    @ConcordKey
    private final String name;

    @NotNull
    @Size(max = 2048)
    private final String url;

    @Size(max = 255)
    private final String branch;

    @Size(max = 64)
    private final String commitId;

    @Size(max = 2048)
    private final String path;

    @ConcordKey
    private final String secret;

    private final Long webhookId;

    public RepositoryEntry(String name, String url) {
        this(null, null, name, url, null, null, null, null, null);
    }

    @JsonCreator

    public RepositoryEntry(@JsonProperty("id") UUID id,
                           @JsonProperty("projectId") UUID projectId,
                           @JsonProperty("name") String name,
                           @JsonProperty("url") String url,
                           @JsonProperty("branch") String branch,
                           @JsonProperty("commitId") String commitId,
                           @JsonProperty("path") String path,
                           @JsonProperty("secret") String secret,
                           @JsonProperty("webhookId") Long webhookId) {

        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.url = url;
        this.branch = branch;
        this.commitId = commitId;
        this.path = path;
        this.secret = secret;
        this.webhookId = webhookId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getPath() {
        return path;
    }

    public String getSecret() {
        return secret;
    }

    public Long getWebhookId() {
        return webhookId;
    }

    @Override
    public String toString() {
        return "RepositoryEntry{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", branch='" + branch + '\'' +
                ", commitId='" + commitId + '\'' +
                ", path='" + path + '\'' +
                ", secret='" + secret + '\'' +
                ", webhookId=" + webhookId +
                '}';
    }
}