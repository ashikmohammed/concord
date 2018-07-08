package com.walmartlabs.concord.server.org;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.common.validation.ConcordKey;
import com.walmartlabs.concord.project.InternalConstants;
import com.walmartlabs.concord.server.console.ResponseTemplates;
import com.walmartlabs.concord.server.org.project.ProjectDao;
import com.walmartlabs.concord.server.org.project.RepositoryDao;
import com.walmartlabs.concord.server.process.*;
import com.walmartlabs.concord.server.process.pipelines.processors.RequestInfoProcessor;
import com.walmartlabs.concord.server.process.queue.ProcessQueueDao;
import com.walmartlabs.concord.server.security.UserPrincipal;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.siesta.Resource;
import org.sonatype.siesta.Validate;
import org.sonatype.siesta.ValidationErrorsException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status;

@Named
@Singleton
@Api(value = "Project Processes", authorizations = {@Authorization("api_key"), @Authorization("session_key"), @Authorization("ldap")})
@Path("/api/v1/org")
public class ProjectProcessResource implements Resource {

    private static final Logger log = LoggerFactory.getLogger(ProjectProcessResource.class);

    // TODO replace with pagination
    private static final int DEFAULT_LIST_LIMIT = 100;

    private final ProcessManager processManager;
    private final OrganizationManager orgManager;
    private final OrganizationDao orgDao;
    private final ProcessQueueDao queueDao;
    private final ConcordFormService formService;
    private final ResponseTemplates responseTemplates;
    private final ProjectDao projectDao;
    private final RepositoryDao repositoryDao;

    @Inject
    public ProjectProcessResource(ProcessManager processManager,
                                  OrganizationDao orgDao,
                                  ProcessQueueDao queueDao,
                                  ConcordFormService formService,
                                  OrganizationManager orgManager,
                                  ProjectDao projectDao,
                                  RepositoryDao repositoryDao) {

        this.processManager = processManager;
        this.orgDao = orgDao;
        this.queueDao = queueDao;
        this.formService = formService;
        this.responseTemplates = new ResponseTemplates();
        this.orgManager = orgManager;
        this.projectDao = projectDao;
        this.repositoryDao = repositoryDao;
    }


    @GET
    @ApiOperation("List processes for the specified organization")
    @Path("/{orgName}/process")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProcessEntry> list(@ApiParam @PathParam("orgName") @ConcordKey String orgName) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);
        return queueDao.list(Collections.singleton(org.getId()), false, null, null, null, DEFAULT_LIST_LIMIT);
    }

    @GET
    @ApiOperation("List processes for the specified project")
    @Path("/{orgName}/project/{projectName}/process")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProcessEntry> list(@ApiParam @PathParam("orgName") @ConcordKey String orgName,
                                   @ApiParam @PathParam("projectName") @ConcordKey String projectName) {

        OrganizationEntry org = orgManager.assertAccess(orgName, false);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Response.Status.NOT_FOUND);
        }

        return queueDao.list(null, false, projectId, null, null, DEFAULT_LIST_LIMIT);
    }

    /**
     * Starts a new process instance.
     *
     * @param orgName
     * @param projectName
     * @param repoName
     * @param entryPoint
     * @param activeProfiles
     * @return
     */
    @GET
    @ApiOperation("Start a new process")
    @Path("/{orgName}/project/{projectName}/repo/{repoName}/start/{entryPoint}")
    @Validate
    public Response start(@ApiParam @PathParam("orgName") String orgName,
                          @ApiParam @PathParam("projectName") String projectName,
                          @ApiParam @PathParam("repoName") String repoName,
                          @ApiParam @PathParam("entryPoint") String entryPoint,
                          @ApiParam @QueryParam("activeProfiles") String activeProfiles,
                          @Context UriInfo uriInfo) {

        try {
            return doStartProcess(orgName, projectName, repoName, entryPoint, activeProfiles, uriInfo);
        } catch (Exception e) {
            log.error("startProcess ['{}', '{}', '{}', '{}', '{}'] -> error",
                    orgName, projectName, repoName, entryPoint, activeProfiles, e);
            return processError(null, "Process error: " + e.getMessage());
        }
    }

    private Response doStartProcess(String orgName, String projectName, String repoName, String entryPoint, String activeProfiles, UriInfo uriInfo) {
        Map<String, Object> req = new HashMap<>();
        if (activeProfiles != null) {
            String[] as = activeProfiles.split(",");
            req.put(InternalConstants.Request.ACTIVE_PROFILES_KEY, Arrays.asList(as));
        }

        if (uriInfo != null) {
            Map<String, Object> args = new HashMap<>();
            args.put("requestInfo", RequestInfoProcessor.createRequestInfo(uriInfo));
            args.putAll(parseArguments(uriInfo));
            req.put(InternalConstants.Request.ARGUMENTS_KEY, args);
        }

        UUID instanceId = UUID.randomUUID();

        try {
            UUID orgId = getOrgId(orgName);
            UUID projectId = getProjectId(orgId, projectName);
            UUID repoId = getRepoId(projectId, repoName);

            Payload payload = new PayloadBuilder(instanceId)
                    .organization(orgId)
                    .project(projectId)
                    .repository(repoId)
                    .entryPoint(entryPoint)
                    .initiator(getInitiator())
                    .configuration(req)
                    .build();

            Thread processStartThread = new Thread(() -> processManager.start(payload, false));
            processStartThread.start();

            waitTillProcessIsInitialized(instanceId);
        } catch (Exception e) {
            return processError(instanceId, e.getMessage());
        }

        return proceed(instanceId);
    }

    @POST
    @ApiOperation("Proceed to next step for the process")
    @Path("{processInstanceId}/next")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proceed(@PathParam("processInstanceId") UUID processInstanceId) {

        ProcessEntry entry = queueDao.get(processInstanceId);

        ProcessStatus processStatus = entry.getStatus();
        if (processStatus == ProcessStatus.FAILED || processStatus == ProcessStatus.CANCELLED) {
            return processError(processInstanceId, "Process failed");
        } else if (processStatus == ProcessStatus.FINISHED) {
            return processFinished(processInstanceId);
        } else if (processStatus == ProcessStatus.SUSPENDED) {
            String nextFormId = formService.nextFormId(processInstanceId);
            if (nextFormId == null) {
                return processError(processInstanceId, "Invalid process state: no forms found");
            }

            String url = "/#/process/" + entry.getInstanceId() + "/wizard";
            return Response.status(Status.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } else {
            Map<String, Object> args = prepareArgumentsForInProgressTemplate(entry);
            return responseTemplates.inProgressWait(Response.ok(), args).build();
        }
    }

    private void waitTillProcessIsInitialized(UUID instanceId) throws InterruptedException {
        ProcessEntry entry = queueDao.get(instanceId);
        while (entry == null) {
            TimeUnit.MILLISECONDS.sleep(100);
            entry = queueDao.get(instanceId);
        }
    }

    private static Map<String, Object> prepareArgumentsForInProgressTemplate(ProcessEntry entry) {
        Map<String, Object> args = new HashMap<>();
        args.put("orgName", entry.getOrgName());
        args.put("projectName", entry.getProjectName());
        args.put("instanceId", entry.getInstanceId().toString());
        args.put("parentInstanceId", entry.getParentInstanceId());
        args.put("initiator", entry.getInitiator());
        args.put("createdAt", entry.getCreatedAt());
        args.put("lastUpdatedAt", entry.getLastUpdatedAt());
        args.put("status", entry.getStatus().toString());
        return args;
    }

    private static Map<String, Object> parseArguments(UriInfo uriInfo) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters(true).entrySet()) {
            String k = e.getKey();
            if (!k.startsWith("arguments.")) {
                continue;
            }
            k = k.substring("arguments.".length());

            Object v = e.getValue();
            if (e.getValue().size() == 1) {
                v = e.getValue().get(0);
            }

            Map<String, Object> m = ConfigurationUtils.toNested(k, v);
            result = ConfigurationUtils.deepMerge(result, m);
        }

        return result;
    }

    private UUID getOrgId(String orgName) {
        UUID id = orgDao.getId(orgName);
        if (id == null) {
            throw new ValidationErrorsException("Organization not found: " + orgName);
        }
        return id;
    }

    private UUID getProjectId(UUID orgId, String projectName) {
        if (projectName == null) {
            return null;
        }

        if (orgId == null) {
            throw new ValidationErrorsException("Organization name is required");
        }

        UUID id = projectDao.getId(orgId, projectName);
        if (id == null) {
            throw new ValidationErrorsException("Project not found: " + projectName);
        }
        return id;
    }

    private UUID getRepoId(UUID projectId, String repoName) {
        if (repoName == null) {
            return null;
        }

        if (projectId == null) {
            throw new ValidationErrorsException("Project name is required");
        }

        UUID id = repositoryDao.getId(projectId, repoName);
        if (id == null) {
            throw new ValidationErrorsException("Repository not found: " + repoName);
        }
        return id;
    }

    private ProcessEntry getProcess(UUID instanceId) {
        ProcessEntry e = queueDao.get(instanceId);
        if (e == null) {
            log.warn("getProcess ['{}'] -> not found", instanceId);
            throw new WebApplicationException("Process instance not found", Status.NOT_FOUND);
        }
        return e;
    }

    private Response processFinished(UUID instanceId) {
        return responseTemplates.processFinished(Response.ok(),
                Collections.singletonMap("instanceId", instanceId))
                .build();
    }

    private Response processError(UUID instanceId, String message) {
        Map<String, Object> args = new HashMap<>();
        if (instanceId != null) {
            args.put("instanceId", instanceId);
        }
        args.put("message", message);

        return responseTemplates.processError(Response.status(Status.INTERNAL_SERVER_ERROR), args)
                .build();
    }

    private static String getInitiator() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null || !subject.isAuthenticated()) {
            return null;
        }

        UserPrincipal p = (UserPrincipal) subject.getPrincipal();
        return p.getUsername();
    }
}