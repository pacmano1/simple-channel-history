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
    public List<RevisionInfo> getHistory(String fileName) throws ClientException {
        try {
            return repo.getChannelHistory(fileName);
        }
        catch(Exception e) {
            log.warn("failed to get the history of file {}", fileName, e);
            throw new ClientException(e);
        }
    }

    @Override
    public String getContent(String fileName, String revision) throws ClientException {
        try {
            return repo.getChannelContent(fileName, revision);
        }
        catch(Exception e) {
            log.warn("failed to get the content of file {} at revision {}", fileName, revision, e);
            throw new ClientException(e);
        }
    }

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

            String desc = channel.getDescription();
            desc = desc + "\n(" + "reverted to revision " + revision + ")";
            channel.setDescription(desc);
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
