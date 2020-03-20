package com.redhat.labs.omp.resources;

import com.redhat.labs.cache.cacheStore.ResidencyDataCache;
import com.redhat.labs.omp.models.GetFileResponse;
import com.redhat.labs.omp.models.filesmanagement.SingleFileResponse;
import com.redhat.labs.omp.resources.filters.Logged;
import com.redhat.labs.omp.services.GitLabService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Path("/api/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FileResource {
    public static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

    @Inject
    @RestClient
    protected GitLabService gitLabService;
    
    @Inject
    private ResidencyDataCache cache;

    @GET
    @Logged
    public SingleFileResponse getFileFromGitByName(@QueryParam("name") String fileName, @QueryParam("repo_id") String repoId, @QueryParam("branch") String branch) {
        return this.fetchContentFromGit(fileName, repoId, branch);
    }

    public SingleFileResponse fetchContentFromGit(String fileName, String templateRepositoryId, String branch) {
        LOGGER.debug(String.format("Template Repo %s filename %s branch %s", templateRepositoryId, fileName, branch));

        GetFileResponse metaFileResponse = gitLabService.getFile(templateRepositoryId, fileName, branch == null ? "master" : branch);
        String base64Content = metaFileResponse.content;
        String content = new String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8);
        LOGGER.debug("File {} content fetched {}", fileName, content);
        SingleFileResponse response = new SingleFileResponse(fileName, content);
        if(content != null) {
            String cacheJson = JsonbBuilder.create().toJson(response);
            LOGGER.info("adding {} to cache", fileName);
            cache.store(fileName, cacheJson);
        }
        return response;
    }

    public SingleFileResponse fetchContentFromGit(String fileName, String templateRepositoryId) {
        return this.fetchContentFromGit(fileName, templateRepositoryId, null);
    }
}