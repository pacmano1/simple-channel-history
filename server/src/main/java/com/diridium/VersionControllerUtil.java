package com.diridium;

import com.mirth.connect.model.ServerEventContext;

/**
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class VersionControllerUtil {

    public VersionControllerUtil() {
    }

    public int getUserId(ServerEventContext sec) {
        return sec.getUserId();
    }
}
