/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

import java.lang.reflect.Method;

public class TestDiscoveryListener implements TestListener {
    @Override
    public void addError(Test test, Throwable t) {
    }

    @Override
    public void addFailure(Test test, AssertionFailedError t) {
    }

    @Override
    public void startTest(Test test) {
        try {
            final Object data = getData();
            Method testStarted = data.getClass().getMethod("testDiscoveryStarted", String.class, String.class);
            testStarted.invoke(data, getClassName(test), getMethodName(test));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void endTest(Test test) {
        final String className = getClassName(test);
        final String methodName = getMethodName(test);

        try {
            final Object data = getData();
            Method testEnded = data.getClass().getMethod("testDiscoveryEnded", String.class, String.class);
            testEnded.invoke(data, className, methodName);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String getMethodName(Test test) {
        final String toString = test.toString();
        final int braceIdx = toString.indexOf("(");
        return braceIdx > 0 ? toString.substring(0, braceIdx) : toString;
    }

    private static String getClassName(Test test) {
        final String toString = test.toString();
        final int braceIdx = toString.indexOf("(");
        return braceIdx > 0 && toString.endsWith(")") ? toString.substring(braceIdx + 1, toString.length() - 1) : null;
    }

    public Object getData() throws Exception {
        return Class.forName("com.intellij.rt.coverage.data.TestDiscoveryProjectData")
                .getMethod("getProjectData")
                .invoke(null);
    }
}
