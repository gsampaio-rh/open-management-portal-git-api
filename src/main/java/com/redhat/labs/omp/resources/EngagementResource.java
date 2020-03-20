package com.redhat.labs.omp.resources;


import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.models.CreateResidencyGroupStructure;
import com.redhat.labs.omp.models.FileAction;
import com.redhat.labs.omp.models.GitLabCreateProjectResponse;
import com.redhat.labs.omp.models.Residency;
import com.redhat.labs.omp.models.filesmanagement.CommitMultipleFilesInRepsitoryRequest;
import com.redhat.labs.omp.models.filesmanagement.CreateCommitFileRequest;
import com.redhat.labs.omp.models.filesmanagement.GetMultipleFilesResponse;
import com.redhat.labs.omp.services.GitLabService;
import com.redhat.labs.omp.utils.TemplateCombobulator;

@Path("/api/residencies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EngagementResource {
    public static Logger LOGGER = LoggerFactory.getLogger(EngagementResource.class);

    @ConfigProperty(name = "stripPathPrefix", defaultValue = "schema/")
    protected String stripPathPrefix;

    @Inject
    protected TemplateCombobulator combobulator;

    @Inject
    protected ProjectsResource projects;

    @Inject
    protected GroupsResource groups;

    @Inject
    @RestClient
    protected GitLabService gitLabService;

    @POST
    @Counted(name = "residencies", description = "How many residencies request have been requested")
    @Timed(name = "performedChecks", description = "How much time it takes to create residency", unit = MetricUnits.MILLISECONDS)
    public Response createResidency(Residency residency, @Context UriInfo uriInfo) {
        GitLabCreateProjectResponse gitLabCreateProjectResponse = createGitLabProject(residency);
        residency.id = gitLabCreateProjectResponse.id;

        GetMultipleFilesResponse getMultipleFilesResponse = combobulator.process(residency.toMap());

        CommitMultipleFilesInRepsitoryRequest commitMultipleFilesInRepsitoryRequest = getCommitMultipleFilesInRepositoryRequest(residency, getMultipleFilesResponse);

        Response gitResponse = gitLabService.createFilesInRepository(gitLabCreateProjectResponse.id, commitMultipleFilesInRepsitoryRequest);

        if(gitResponse.getStatus() == 201) {
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(Integer.toString(gitLabCreateProjectResponse.id));
            return Response.created(builder.build()).build();
        }

        return gitResponse;

    }

    private CommitMultipleFilesInRepsitoryRequest getCommitMultipleFilesInRepositoryRequest(Residency residency, GetMultipleFilesResponse getMultipleFilesResponse) {
        CommitMultipleFilesInRepsitoryRequest commitMultipleFilesInRepsitoryRequest = new CommitMultipleFilesInRepsitoryRequest();
        getMultipleFilesResponse.files.stream().forEach(f -> {
            commitMultipleFilesInRepsitoryRequest.addFileRequest(new CreateCommitFileRequest(FileAction.create, stripPrefix(f.fileName), f.fileContent));
        });
        commitMultipleFilesInRepsitoryRequest.authorEmail = residency.engagementLeadEmail;
        commitMultipleFilesInRepsitoryRequest.authorName = residency.engagementLeadName;
        commitMultipleFilesInRepsitoryRequest.commitMessage = "\uD83E\uDD84 Created by OMP Git API \uD83D\uDE80 \uD83C\uDFC1";
        return commitMultipleFilesInRepsitoryRequest;
    }

    public String stripPrefix(String in) {
        if (in != null && in.startsWith(stripPathPrefix)) {
            return in.split(stripPathPrefix)[1];
        }
        return in;
    }

    private GitLabCreateProjectResponse createGitLabProject(Residency residency) {
        CreateResidencyGroupStructure createResidencyGroupStructure = new CreateResidencyGroupStructure();
        createResidencyGroupStructure.projectName = residency.projectName;
        createResidencyGroupStructure.customerName = residency.customerName;
        return groups.createResidencyStructure(createResidencyGroupStructure);
    }
}
