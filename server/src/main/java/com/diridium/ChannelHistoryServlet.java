// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.model.Channel;
import com.mirth.connect.model.ChannelMetadata;
import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.server.api.CheckAuthorizedChannelId;
import com.mirth.connect.server.api.MirthServlet;
import com.mirth.connect.server.controllers.ChannelController;
import com.mirth.connect.server.controllers.CodeTemplateController;
import com.mirth.connect.server.controllers.ControllerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class ChannelHistoryServlet extends MirthServlet implements ChannelHistoryServletInterface {

    private static Logger log = LoggerFactory.getLogger(ChannelHistoryServlet.class);

    private DatabaseHistoryRepository repo;

    private ChannelController channelController;
    private CodeTemplateController codeTemplateController;

    public ChannelHistoryServlet(@Context HttpServletRequest request, @Context SecurityContext sc) {
        super(request, sc, PLUGIN_NAME);
        repo = DatabaseHistoryRepository.getInstance();
        channelController = ChannelController.getInstance();
        codeTemplateController = ControllerFactory.getFactory().createCodeTemplateController();
    }

    @Override
    @CheckAuthorizedChannelId
    public List<RevisionInfo> getHistory(String channelId) throws ClientException {
        try {
            return repo.getChannelHistory(channelId);
        }
        catch(Exception e) {
            log.warn("failed to get the history of channel {}", channelId, e);
            throw new ClientException(e);
        }
    }

    @Override
    @CheckAuthorizedChannelId
    public String getContent(String channelId, String revision) throws ClientException {
        try {
            return repo.getChannelContent(channelId, revision);
        }
        catch(Exception e) {
            log.warn("failed to get the content of channel {} at revision {}", channelId, revision, e);
            throw new ClientException(e);
        }
    }

    private static final String REVERT_HISTORY_BEGIN = "--- BEGIN REVERT HISTORY (do not delete these tags) ---";
    private static final String REVERT_HISTORY_END = "--- END REVERT HISTORY ---";

    @Override
    @CheckAuthorizedChannelId
    public boolean revertChannel(String channelId, String revision) throws ClientException {
        try {
            Channel channel = repo.getChannelAtRevision(channelId, revision);
            ChannelMetadata metadata = channel.getExportData().getMetadata();
            if(metadata == null) {
                metadata = new ChannelMetadata();
                channel.getExportData().setMetadata(metadata);
            }
            // without this the userId will be null and will result in a warning
            // on the client when user tries to save the same channel after reverting
            metadata.setUserId(context.getUserId());

            // Get original description from the revision we're reverting to
            String oldDesc = channel.getDescription();
            if (oldDesc == null) {
                oldDesc = "";
            }
            // Remove any existing revert history from the old description
            int beginIdx = oldDesc.indexOf(REVERT_HISTORY_BEGIN);
            if (beginIdx >= 0) {
                oldDesc = oldDesc.substring(0, beginIdx).trim();
            }

            // Extract existing revert history from current channel (if any)
            String existingHistory = "";
            Channel currentChannel = channelController.getChannelById(channelId);
            if (currentChannel != null && currentChannel.getDescription() != null) {
                String currentDesc = currentChannel.getDescription();
                int currentBegin = currentDesc.indexOf(REVERT_HISTORY_BEGIN);
                int currentEnd = currentDesc.indexOf(REVERT_HISTORY_END);
                if (currentBegin >= 0 && currentEnd > currentBegin) {
                    existingHistory = currentDesc.substring(currentBegin + REVERT_HISTORY_BEGIN.length(), currentEnd).trim();
                }
            }

            // Build new revert entry using the revision number from the history table
            String username = repo.getUserName(context.getUserId());
            int revisionNumber = repo.getChannelRevisionNumber(channelId, revision);
            String newEntry = "(reverted to Rev " + revisionNumber + " by " + username + " at " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + ")";

            // Combine: old description + revert history section at the end (newest first)
            StringBuilder newDesc = new StringBuilder(oldDesc);
            newDesc.append("\n\n").append(REVERT_HISTORY_BEGIN).append("\n");
            newDesc.append(newEntry).append("\n");
            if (!existingHistory.isEmpty()) {
                newDesc.append(existingHistory).append("\n");
            }
            newDesc.append(REVERT_HISTORY_END);

            channel.setDescription(newDesc.toString());
            boolean result = channelController.updateChannel(channel, context, true, Calendar.getInstance());
            log.debug("reverted Channel {} to revision {}", channelId, revision);
            return result;
        }
        catch (Exception e) {
            log.warn("failed to revert Channel {} to revision {}", channelId, revision);
            throw new ClientException(e);
        }
    }

    @Override
    public List<RevisionInfo> getCodeTemplateHistory(String codeTemplateId) throws ClientException {
        try {
            return repo.getCodeTemplateHistory(codeTemplateId);
        }
        catch(Exception e) {
            log.warn("failed to get the history of code template {}", codeTemplateId, e);
            throw new ClientException(e);
        }
    }

    @Override
    public String getCodeTemplateContent(String codeTemplateId, String revision) throws ClientException {
        try {
            return repo.getCodeTemplateContent(codeTemplateId, revision);
        }
        catch(Exception e) {
            log.warn("failed to get the content of code template {} at revision {}", codeTemplateId, revision, e);
            throw new ClientException(e);
        }
    }

    @Override
    public boolean revertCodeTemplate(String codeTemplateId, String revision) throws ClientException {
        try {
            CodeTemplate codeTemplate = repo.getCodeTemplateAtRevision(codeTemplateId, revision);

            // Update the code template
            codeTemplateController.updateCodeTemplate(codeTemplate, context, true);
            log.debug("reverted CodeTemplate {} to revision {}", codeTemplateId, revision);
            return true;
        }
        catch (Exception e) {
            log.warn("failed to revert CodeTemplate {} to revision {}", codeTemplateId, revision, e);
            throw new ClientException(e);
        }
    }

    @Override
    @CheckAuthorizedChannelId
    public int pruneChannelHistory(String channelId, String revision) throws ClientException {
        try {
            int deleted = repo.pruneChannelHistoryOlderThan(channelId, revision);
            log.info("Pruned {} older revisions for channel {}", deleted, channelId);
            return deleted;
        }
        catch (Exception e) {
            log.warn("failed to prune channel history for {} at revision {}", channelId, revision, e);
            throw new ClientException(e);
        }
    }

    @Override
    public int pruneCodeTemplateHistory(String codeTemplateId, String revision) throws ClientException {
        try {
            int deleted = repo.pruneCodeTemplateHistoryOlderThan(codeTemplateId, revision);
            log.info("Pruned {} older revisions for code template {}", deleted, codeTemplateId);
            return deleted;
        }
        catch (Exception e) {
            log.warn("failed to prune code template history for {} at revision {}", codeTemplateId, revision, e);
            throw new ClientException(e);
        }
    }
}
