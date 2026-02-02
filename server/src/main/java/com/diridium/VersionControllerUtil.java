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
