/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.support;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public final class ResourcePaths {

    private ResourcePaths() {
    }

    public static String fixturesHome() {
        return ResourcePaths.class.getResource("/").getPath();
    }

    public static Path fixturesHome(Class clazz, String path) {
        System.out.println(format("clazz.getResource(%s) -> ", path) + clazz.getResource(path));
        return Paths.get(clazz.getResource(path).getPath());
    }
}
