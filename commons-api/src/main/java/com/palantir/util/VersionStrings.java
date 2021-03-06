/**
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.util;

import java.util.Iterator;

import com.google.common.base.Splitter;

public class VersionStrings {

    public static int compareVersions(String v1, String v2) {
        Iterator<String> v1Iter = Splitter.on('.').trimResults().split(v1).iterator();
        Iterator<String> v2Iter = Splitter.on('.').trimResults().split(v2).iterator();
        while (v1Iter.hasNext() && v2Iter.hasNext()) {
            int v1Part = Integer.parseInt(v1Iter.next());
            int v2Part = Integer.parseInt(v2Iter.next());
            if (v1Part != v2Part) {
                return v1Part - v2Part;
            }
        }
        while (v1Iter.hasNext()) {
            if (Integer.parseInt(v1Iter.next()) != 0) {
                return 1;
            }
        }
        while (v2Iter.hasNext()) {
            if (Integer.parseInt(v2Iter.next()) != 0) {
                return -1;
            }
        }
        return 0;
    }
}
