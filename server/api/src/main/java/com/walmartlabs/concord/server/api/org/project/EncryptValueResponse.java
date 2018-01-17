package com.walmartlabs.concord.server.api.org.project;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 Wal-Mart Store, Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Arrays;

public class EncryptValueResponse implements Serializable {

    private final boolean ok = true;
    private final byte[] data;

    @JsonCreator
    public EncryptValueResponse(@JsonProperty("data") byte[] data) {
        this.data = data;
    }

    public boolean isOk() {
        return ok;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "EncryptValueResponse{" +
                "ok=" + ok +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}