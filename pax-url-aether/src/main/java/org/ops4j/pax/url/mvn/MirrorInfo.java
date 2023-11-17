/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn;

/**
 * Object <em>mirroring</em> Maven's <code>org.apache.maven.settings.Mirror</code> class.
 * This class is part of org.ops4j.pax.url.mvn public API.
 */
public class MirrorInfo {

    private String id = "default";

    /**
     * The server ID of the repository being mirrored, e.g., "central". This MUST NOT match the mirror id.
     */
    private String mirrorOf;

    /**
     * The optional name that describes the mirror.
     */
    private String name;

    /**
     * The URL of the mirror repository.
     */
    private String url;

    /**
     * The layout of the mirror repository. Since Maven 3.
     */
    private String layout = "default";

    /**
     * The layouts of repositories being mirrored. This value can be used to restrict the usage
     * of the mirror to repositories with a matching layout (apart from a matching id). Since Maven 3.
     */
    private String mirrorOfLayouts = "default,legacy";

    public MirrorInfo() {
    }

    public MirrorInfo(String id, String url, String mirrorOf) {
        this.id = id;
        this.url = url;
        this.mirrorOf = mirrorOf;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMirrorOf() {
        return mirrorOf;
    }

    public void setMirrorOf(String mirrorOf) {
        this.mirrorOf = mirrorOf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getMirrorOfLayouts() {
        return mirrorOfLayouts;
    }

    public void setMirrorOfLayouts(String mirrorOfLayouts) {
        this.mirrorOfLayouts = mirrorOfLayouts;
    }

}
