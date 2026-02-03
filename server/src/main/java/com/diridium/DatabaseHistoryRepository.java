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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mirth.connect.donkey.server.data.DonkeyDaoFactory;
import com.mirth.connect.model.Channel;
import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.UserController;
import com.mirth.connect.server.util.SqlConfig;

/**
 * Database-backed repository for storing channel and code template history.
 */
public class DatabaseHistoryRepository {

    private static DatabaseHistoryRepository instance;
    private static final Logger log = LoggerFactory.getLogger(DatabaseHistoryRepository.class);

    private ObjectXMLSerializer serializer;
    private UserController userController;

    private DatabaseHistoryRepository() {
    }

    public static synchronized void init(ObjectXMLSerializer serializer) {
        if (instance == null) {
            instance = new DatabaseHistoryRepository();
            instance.serializer = serializer;
            instance.userController = ControllerFactory.getFactory().createUserController();
            log.info("DatabaseHistoryRepository initialized");
        }
    }

    public static DatabaseHistoryRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseHistoryRepository not initialized, call init() first");
        }
        return instance;
    }

    public static synchronized void close() {
        instance = null;
    }

    // ========== Channel History Methods ==========

    public void saveChannelHistory(Channel channel, int userId) {
        String sql = "INSERT INTO channel_history (revision, channel_id, user_id, date_created, channel) VALUES (?, ?, ?, ?, ?)";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String xml = serializer.serialize(channel);
            ps.setInt(1, channel.getRevision());
            ps.setString(2, channel.getId());
            ps.setInt(3, userId);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setString(5, xml);

            ps.executeUpdate();
            log.debug("Saved channel history for channel {} revision {}", channel.getId(), channel.getRevision());
        } catch (SQLException e) {
            // Fail silent - don't block channel save if history save fails
            log.error("Failed to save channel history for channel {}", channel.getId(), e);
        }
    }

    public List<RevisionInfo> getChannelHistory(String channelId) {
        String sql = "SELECT id, revision, user_id, date_created FROM channel_history WHERE channel_id = ? ORDER BY id DESC";
        List<RevisionInfo> history = new ArrayList<>();

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RevisionInfo ri = new RevisionInfo();
                    ri.setHash(String.valueOf(rs.getLong("id")));
                    ri.setRevision(rs.getInt("revision"));
                    ri.setTime(rs.getTimestamp("date_created").getTime());
                    ri.setCommitterName(getUserName(rs.getInt("user_id")));
                    history.add(ri);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get channel history for {}", channelId, e);
            throw new RuntimeException(e);
        }

        return history;
    }

    public String getChannelContent(String channelId, String historyId) {
        String sql = "SELECT channel FROM channel_history WHERE id = ? AND channel_id = ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, Long.parseLong(historyId));
            ps.setString(2, channelId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("channel");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get channel content for {} at history {}", channelId, historyId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public int getChannelRevisionNumber(String channelId, String historyId) {
        String sql = "SELECT revision FROM channel_history WHERE id = ? AND channel_id = ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, Long.parseLong(historyId));
            ps.setString(2, channelId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("revision");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get revision number for {} at history {}", channelId, historyId, e);
        }

        return -1;
    }

    public Channel getChannelAtRevision(String channelId, String historyId) throws Exception {
        // First verify this isn't the latest entry
        List<RevisionInfo> history = getChannelHistory(channelId);
        if (!history.isEmpty() && history.get(0).getHash().equals(historyId)) {
            throw new IllegalArgumentException("cannot revert to the same revision");
        }

        String content = getChannelContent(channelId, historyId);
        if (content == null) {
            throw new IllegalArgumentException("no history " + historyId + " of Channel " + channelId + " exists");
        }

        return serializer.deserialize(content, Channel.class);
    }

    public void deleteChannelHistory(String channelId) {
        String sql = "DELETE FROM channel_history WHERE channel_id = ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, channelId);
            int deleted = ps.executeUpdate();
            log.debug("Deleted {} history entries for channel {}", deleted, channelId);
        } catch (SQLException e) {
            // Fail silent - don't block channel delete if history cleanup fails
            log.error("Failed to delete channel history for {}", channelId, e);
        }
    }

    // ========== Code Template History Methods ==========

    public void saveCodeTemplateHistory(CodeTemplate codeTemplate, int userId) {
        String sql = "INSERT INTO code_template_history (revision, code_template_id, user_id, date_created, code_template) VALUES (?, ?, ?, ?, ?)";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String xml = serializer.serialize(codeTemplate);
            ps.setInt(1, codeTemplate.getRevision());
            ps.setString(2, codeTemplate.getId());
            ps.setInt(3, userId);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setString(5, xml);

            ps.executeUpdate();
            log.debug("Saved code template history for {} revision {}", codeTemplate.getId(), codeTemplate.getRevision());
        } catch (SQLException e) {
            // Fail silent - don't block code template save if history save fails
            log.error("Failed to save code template history for {}", codeTemplate.getId(), e);
        }
    }

    public List<RevisionInfo> getCodeTemplateHistory(String codeTemplateId) {
        String sql = "SELECT id, revision, user_id, date_created FROM code_template_history WHERE code_template_id = ? ORDER BY id DESC";
        List<RevisionInfo> history = new ArrayList<>();

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codeTemplateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RevisionInfo ri = new RevisionInfo();
                    ri.setHash(String.valueOf(rs.getLong("id")));
                    ri.setRevision(rs.getInt("revision"));
                    ri.setTime(rs.getTimestamp("date_created").getTime());
                    ri.setCommitterName(getUserName(rs.getInt("user_id")));
                    history.add(ri);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get code template history for {}", codeTemplateId, e);
            throw new RuntimeException(e);
        }

        return history;
    }

    public String getCodeTemplateContent(String codeTemplateId, String historyId) {
        String sql = "SELECT code_template FROM code_template_history WHERE id = ? AND code_template_id = ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, Long.parseLong(historyId));
            ps.setString(2, codeTemplateId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code_template");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get code template content for {} at history {}", codeTemplateId, historyId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public void deleteCodeTemplateHistory(String codeTemplateId) {
        String sql = "DELETE FROM code_template_history WHERE code_template_id = ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codeTemplateId);
            int deleted = ps.executeUpdate();
            log.debug("Deleted {} history entries for code template {}", deleted, codeTemplateId);
        } catch (SQLException e) {
            // Fail silent - don't block code template delete if history cleanup fails
            log.error("Failed to delete code template history for {}", codeTemplateId, e);
        }
    }

    public CodeTemplate getCodeTemplateAtRevision(String codeTemplateId, String historyId) throws Exception {
        // First verify this isn't the latest entry
        List<RevisionInfo> history = getCodeTemplateHistory(codeTemplateId);
        if (!history.isEmpty() && history.get(0).getHash().equals(historyId)) {
            throw new IllegalArgumentException("cannot revert to the same revision");
        }

        String content = getCodeTemplateContent(codeTemplateId, historyId);
        if (content == null) {
            throw new IllegalArgumentException("no history " + historyId + " of CodeTemplate " + codeTemplateId + " exists");
        }

        return serializer.deserialize(content, CodeTemplate.class);
    }

    // ========== Prune Methods ==========

    public int pruneChannelHistoryOlderThan(String channelId, String historyId) {
        String sql = "DELETE FROM channel_history WHERE channel_id = ? AND id < ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, channelId);
            ps.setLong(2, Long.parseLong(historyId));
            int deleted = ps.executeUpdate();
            log.info("Pruned {} older history entries for channel {}", deleted, channelId);
            return deleted;
        } catch (SQLException e) {
            log.error("Failed to prune channel history for {}", channelId, e);
            throw new RuntimeException(e);
        }
    }

    public int pruneCodeTemplateHistoryOlderThan(String codeTemplateId, String historyId) {
        String sql = "DELETE FROM code_template_history WHERE code_template_id = ? AND id < ?";

        try (SqlSession session = SqlConfig.getInstance().getSqlSessionManager().openSession(true);
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codeTemplateId);
            ps.setLong(2, Long.parseLong(historyId));
            int deleted = ps.executeUpdate();
            log.info("Pruned {} older history entries for code template {}", deleted, codeTemplateId);
            return deleted;
        } catch (SQLException e) {
            log.error("Failed to prune code template history for {}", codeTemplateId, e);
            throw new RuntimeException(e);
        }
    }

    // ========== Helper Methods ==========

    public String getUserName(int userId) {
        try {
            com.mirth.connect.model.User user = userController.getUser(userId, null);
            if (user != null) {
                return user.getUsername();
            }
        } catch (Exception e) {
            log.debug("Could not get username for user {}", userId, e);
        }
        return "Unknown";
    }
}
