package com.diridium;

import com.mirth.connect.model.Channel;
import com.mirth.connect.model.ServerEventContext;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ChannelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class ChannelVersionController implements ChannelPlugin {

    private static Logger log = LoggerFactory.getLogger(ChannelVersionController.class);

    private DatabaseHistoryRepository repo;

    private VersionControllerUtil vcUtil;

    @Override
    public String getPluginPointName() {
        return ChannelHistoryServletInterface.PLUGIN_NAME;
    }

    @Override
    public void start() {
        log.info("starting simple-channel-history channel version controller");
        vcUtil = new VersionControllerUtil();
        ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
        DatabaseHistoryRepository.init(serializer);
        repo = DatabaseHistoryRepository.getInstance();
    }

    @Override
    public void stop() {
        DatabaseHistoryRepository.close();
    }

    @Override
    public void save(Channel channel, ServerEventContext sec) {
        log.info("saving channel {} by user {}", channel.getId(), sec.getUserId());
        repo.saveChannelHistory(channel, vcUtil.getUserId(sec));
    }

    @Override
    public void remove(Channel channel, ServerEventContext sec) {
        repo.deleteChannelHistory(channel.getId());
    }

    @Override
    public void deploy(Channel channel, ServerEventContext arg1) {
    }

    @Override
    public void deploy(ServerEventContext sec) {
    }

    @Override
    public void undeploy(ServerEventContext sec) {
    }

    @Override
    public void undeploy(String id, ServerEventContext sec) {
    }
}
