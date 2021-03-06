package com.walmartlabs.concord.server.process.pipelines.processors;

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

import com.walmartlabs.concord.project.InternalConstants;
import com.walmartlabs.concord.server.process.pipelines.processors.signing.Signing;
import com.walmartlabs.concord.server.security.ldap.LdapManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CurrentUserInfoProcessor extends UserInfoProcessor {

    @Inject
    public CurrentUserInfoProcessor(LdapManager ldapManager, Signing signing) {
        super(InternalConstants.Request.CURRENT_USER_KEY, ldapManager, signing);
    }
}
