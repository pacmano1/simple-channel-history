package com.diridium;

/*
   Copyright [2024] [Kiran Ayyagari]
   Copyright [2025-2026] [Diridium Technologies Inc.]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.mirth.connect.client.core.Operation.ExecuteType;
import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.core.Permissions;
import com.mirth.connect.client.core.api.BaseServletInterface;
import com.mirth.connect.client.core.api.MirthOperation;
import com.mirth.connect.client.core.api.Param;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
@Path("/extensions/simple-channel-history")
@Tag(name = "Simple Channel History Extension")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public interface ChannelHistoryServletInterface extends BaseServletInterface {
    String PLUGIN_NAME = "Simple Channel History";

    @GET
    @Path("/history")
    @Operation(summary = "Returns a List of all revisions of the given channel")
    @MirthOperation(name = "getHistory", display = "Get all revisions of a channel", permission = Permissions.CHANNELS_VIEW, type = ExecuteType.ASYNC, auditable = false)
    List<RevisionInfo> getHistory(@Param("channelId") @Parameter(description = "The ID of the channel", required = true) @QueryParam("channelId") String channelId) throws ClientException;

    @GET
    @Path("/content")
    @Operation(summary = "Returns the content of the given channel at the specified revision")
    @MirthOperation(name = "getContent", display = "Get the content of the channel at a specific revision", permission = Permissions.CHANNELS_VIEW, type = ExecuteType.SYNC, auditable = false)
    String getContent(@Param("channelId") @Parameter(description = "The ID of the channel", required = true) @QueryParam("channelId") String channelId,
            @Param("revision") @Parameter(description = "The revision ID", required = true) @QueryParam("revision") String revision) throws ClientException;

    @POST
    @Path("/revertChannel")
    @Operation(summary = "Revert the given Channel to the specified revision")
    @MirthOperation(name = "revertChannel", display = "Revert the given Channel to the specified revision", permission = Permissions.CHANNELS_MANAGE, type = ExecuteType.SYNC)
    boolean revertChannel(@Param("channelId") @Parameter(description = "The ID of the Channel", required = true) @QueryParam("channelId") String channelId,
            @Param("revision") @Parameter(description = "The value of revision", required = true) @QueryParam("revision") String revision) throws ClientException;

    @GET
    @Path("/codeTemplateHistory")
    @Operation(summary = "Returns a List of all revisions of the given code template")
    @MirthOperation(name = "getCodeTemplateHistory", display = "Get all revisions of a code template", permission = Permissions.CODE_TEMPLATES_VIEW, type = ExecuteType.ASYNC, auditable = false)
    List<RevisionInfo> getCodeTemplateHistory(@Param("codeTemplateId") @Parameter(description = "The ID of the code template", required = true) @QueryParam("codeTemplateId") String codeTemplateId) throws ClientException;

    @GET
    @Path("/codeTemplateContent")
    @Operation(summary = "Returns the content of the given code template at the specified revision")
    @MirthOperation(name = "getCodeTemplateContent", display = "Get the content of the code template at a specific revision", permission = Permissions.CODE_TEMPLATES_VIEW, type = ExecuteType.SYNC, auditable = false)
    String getCodeTemplateContent(@Param("codeTemplateId") @Parameter(description = "The ID of the code template", required = true) @QueryParam("codeTemplateId") String codeTemplateId,
            @Param("revision") @Parameter(description = "The value of revision", required = true) @QueryParam("revision") String revision) throws ClientException;

    @POST
    @Path("/revertCodeTemplate")
    @Operation(summary = "Revert the given CodeTemplate to the specified revision")
    @MirthOperation(name = "revertCodeTemplate", display = "Revert the given CodeTemplate to the specified revision", permission = Permissions.CODE_TEMPLATES_MANAGE, type = ExecuteType.SYNC)
    boolean revertCodeTemplate(@Param("codeTemplateId") @Parameter(description = "The ID of the CodeTemplate", required = true) @QueryParam("codeTemplateId") String codeTemplateId,
            @Param("revision") @Parameter(description = "The value of revision", required = true) @QueryParam("revision") String revision) throws ClientException;

    @POST
    @Path("/pruneChannelHistory")
    @Operation(summary = "Delete channel revisions older than the specified revision")
    @MirthOperation(name = "pruneChannelHistory", display = "Prune older channel revisions", permission = Permissions.CHANNELS_MANAGE, type = ExecuteType.SYNC)
    int pruneChannelHistory(@Param("channelId") @Parameter(description = "The ID of the Channel", required = true) @QueryParam("channelId") String channelId,
            @Param("revision") @Parameter(description = "Keep this revision and newer, delete older", required = true) @QueryParam("revision") String revision) throws ClientException;

    @POST
    @Path("/pruneCodeTemplateHistory")
    @Operation(summary = "Delete code template revisions older than the specified revision")
    @MirthOperation(name = "pruneCodeTemplateHistory", display = "Prune older code template revisions", permission = Permissions.CODE_TEMPLATES_MANAGE, type = ExecuteType.SYNC)
    int pruneCodeTemplateHistory(@Param("codeTemplateId") @Parameter(description = "The ID of the CodeTemplate", required = true) @QueryParam("codeTemplateId") String codeTemplateId,
            @Param("revision") @Parameter(description = "Keep this revision and newer, delete older", required = true) @QueryParam("revision") String revision) throws ClientException;
}
