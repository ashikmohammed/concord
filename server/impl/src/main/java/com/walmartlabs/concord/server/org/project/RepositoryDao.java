package com.walmartlabs.concord.server.org.project;

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

import com.walmartlabs.concord.db.AbstractDao;
import com.walmartlabs.concord.server.api.org.project.RepositoryEntry;
import org.jooq.*;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.UUID;

import static com.walmartlabs.concord.server.jooq.tables.Repositories.REPOSITORIES;
import static com.walmartlabs.concord.server.jooq.tables.Secrets.SECRETS;

@Named
public class RepositoryDao extends AbstractDao {

    @Inject
    public RepositoryDao(Configuration cfg) {
        super(cfg);
    }

    public UUID getId(UUID projectId, String repoName) {
        try (DSLContext tx = DSL.using(cfg)) {
            return tx.select(REPOSITORIES.REPO_ID)
                    .from(REPOSITORIES)
                    .where(REPOSITORIES.PROJECT_ID.eq(projectId)
                            .and(REPOSITORIES.REPO_NAME.eq(repoName)))
                    .fetchOne(REPOSITORIES.REPO_ID);
        }
    }

    public UUID getProjectId(UUID repoId) {
        try (DSLContext tx = DSL.using(cfg)) {
            return tx.select(REPOSITORIES.PROJECT_ID)
                    .from(REPOSITORIES)
                    .where(REPOSITORIES.REPO_ID.eq(repoId))
                    .fetchOne(REPOSITORIES.PROJECT_ID);
        }
    }
    
    public RepositoryEntry get(UUID projectId, UUID repoId) {
        try (DSLContext tx = DSL.using(cfg)) {
            return selectRepositoryEntry(tx)
                    .where(REPOSITORIES.PROJECT_ID.eq(projectId)
                            .and(REPOSITORIES.REPO_ID.eq(repoId)))
                    .fetchOne(RepositoryDao::toEntry);
        }
    }

    public UUID insert(UUID projectId, String repositoryName, String url, String branch, String commitId, String path, UUID secretId, Long hookId) {
        return txResult(tx -> insert(tx, projectId, repositoryName, url, branch, commitId, path, secretId, hookId));
    }

    public UUID insert(DSLContext tx, UUID projectId, String repositoryName, String url, String branch, String commitId, String path, UUID secretId, Long hookId) {
        return tx.insertInto(REPOSITORIES)
                .columns(REPOSITORIES.PROJECT_ID, REPOSITORIES.REPO_NAME,
                        REPOSITORIES.REPO_URL, REPOSITORIES.REPO_BRANCH, REPOSITORIES.REPO_COMMIT_ID,
                        REPOSITORIES.REPO_PATH, REPOSITORIES.SECRET_ID, REPOSITORIES.WEBHOOK_ID)
                .values(projectId, repositoryName, url, branch, commitId, path, secretId, hookId)
                .returning(REPOSITORIES.REPO_ID)
                .fetchOne()
                .getRepoId();
    }

    public void update(DSLContext tx, UUID repoId, String repositoryName, String url, String branch, String commitId, String path, UUID secretId) {
        tx.update(REPOSITORIES)
                .set(REPOSITORIES.REPO_NAME, repositoryName)
                .set(REPOSITORIES.REPO_URL, url)
                .set(REPOSITORIES.SECRET_ID, secretId)
                .set(REPOSITORIES.REPO_BRANCH, branch)
                .set(REPOSITORIES.REPO_COMMIT_ID, commitId)
                .set(REPOSITORIES.REPO_PATH, path)
                .where(REPOSITORIES.REPO_ID.eq(repoId))
                .execute();
    }

    public void delete(DSLContext tx, UUID repoId) {
        tx.deleteFrom(REPOSITORIES)
                .where(REPOSITORIES.REPO_ID.eq(repoId))
                .execute();
    }

    public void deleteAll(DSLContext tx, UUID projectId) {
        tx.deleteFrom(REPOSITORIES)
                .where(REPOSITORIES.PROJECT_ID.eq(projectId))
                .execute();
    }

    public List<RepositoryEntry> list() {
        try (DSLContext tx = DSL.using(cfg)) {
            return selectRepositoryEntry(tx)
                    .fetch(RepositoryDao::toEntry);
        }
    }

    public List<RepositoryEntry> list(UUID projectId) {
        return list(projectId, null, false);
    }

    public List<RepositoryEntry> list(DSLContext tx, UUID projectId) {
        return list(tx, projectId, null, false);
    }

    public List<RepositoryEntry> list(UUID projectId, Field<?> sortField, boolean asc) {
        try (DSLContext tx = DSL.using(cfg)) {
            return list(tx, projectId, sortField, asc);
        }
    }

    public List<RepositoryEntry> list(DSLContext tx, UUID projectId, Field<?> sortField, boolean asc) {
        SelectConditionStep<Record9<UUID, UUID, String, String, String, String, String, Long, String>> query = selectRepositoryEntry(tx)
                .where(REPOSITORIES.PROJECT_ID.eq(projectId));

        if (sortField != null) {
            query.orderBy(asc ? sortField.asc() : sortField.desc());
        }

        return query.fetch(RepositoryDao::toEntry);
    }

    private static SelectJoinStep<Record9<UUID, UUID, String, String, String, String, String, Long, String>> selectRepositoryEntry(DSLContext tx) {
        return tx.select(REPOSITORIES.REPO_ID,
                REPOSITORIES.PROJECT_ID,
                REPOSITORIES.REPO_NAME,
                REPOSITORIES.REPO_URL,
                REPOSITORIES.REPO_BRANCH,
                REPOSITORIES.REPO_COMMIT_ID,
                REPOSITORIES.REPO_PATH,
                REPOSITORIES.WEBHOOK_ID,
                SECRETS.SECRET_NAME)
                .from(REPOSITORIES)
                .leftOuterJoin(SECRETS).on(SECRETS.SECRET_ID.eq(REPOSITORIES.SECRET_ID));
    }

    private static RepositoryEntry toEntry(Record9<UUID, UUID, String, String, String, String, String, Long, String> r) {
        return new RepositoryEntry(r.get(REPOSITORIES.REPO_ID),
                r.get(REPOSITORIES.PROJECT_ID),
                r.get(REPOSITORIES.REPO_NAME),
                r.get(REPOSITORIES.REPO_URL),
                r.get(REPOSITORIES.REPO_BRANCH),
                r.get(REPOSITORIES.REPO_COMMIT_ID),
                r.get(REPOSITORIES.REPO_PATH),
                r.get(SECRETS.SECRET_NAME),
                r.get(REPOSITORIES.WEBHOOK_ID));
    }
}