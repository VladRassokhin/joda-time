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

import java.io.*;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestDiscoveryListener implements TestListener, Closeable {
    private final String myTracesDirectory;
    private final boolean myZipResults;

    public TestDiscoveryListener() {
        myTracesDirectory = System.getProperty("org.jetbrains.instrumentation.trace.dir");
        myZipResults = Boolean.parseBoolean(System.getProperty("org.jetbrains.instrumentation.zip.traces", "true"));
        System.out.println(getClass().getSimpleName() + " instantiated with directory='" + myTracesDirectory + "'");
    }

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
            Method testStarted = data.getClass().getMethod("testDiscoveryStarted", String.class);
            testStarted.invoke(data, getClassName(test) + "-" + getMethodName(test));
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
            Method testEnded = data.getClass().getMethod("testDiscoveryEnded", String.class);
            testEnded.invoke(data, "j" + className + "-" + methodName);
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

    @Override
    public void close() {
        try {
            final Object data = getData();
            Method testEnded = data.getClass().getMethod("closeLog");
            testEnded.invoke(data);
        } catch (Throwable ignored) {
        }
        zipOutput(myTracesDirectory);
    }

    private void zipOutput(String tracesDirectory) {
        if (!myZipResults) return;
        final String zipName = "out.zip";
        final File[] files = new File(tracesDirectory).listFiles((dir, name) -> name != null && !name.equalsIgnoreCase(zipName));
        if (files == null) {
            System.out.println("No traces found.");
            return;
        }
        System.out.println("Preparing zip.");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(new File(tracesDirectory, zipName)))) {
            for (File file : files) {
                addFileToZip(zipOutputStream, file, "/" + file.getName());
            }
            System.out.println("Zip prepared.");

            for (File file : files) {
                file.delete();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Adds a new file entry to the ZIP output stream.
     */
    public static boolean addFileToZip(ZipOutputStream zos, File file, String relativeName) throws IOException {
        while (!relativeName.isEmpty() && relativeName.charAt(0) == '/') {
            relativeName = relativeName.substring(1);
        }

        boolean isDir = file.isDirectory();
        if (isDir && (relativeName.isEmpty() || relativeName.charAt(relativeName.length() - 1) != '/')) {
            relativeName += "/";
        }

        long size = isDir ? 0 : file.length();
        ZipEntry e = new ZipEntry(relativeName);
        e.setTime(file.lastModified());
        if (size == 0) {
            e.setMethod(ZipEntry.STORED);
            e.setSize(0);
            e.setCrc(0);
        }
        zos.putNextEntry(e);
        if (!isDir) {
            try (InputStream is = new FileInputStream(file)) {
                copy(is, zos);
            }
        }
        zos.closeEntry();
        return true;
    }

    private static void copy(InputStream inputStream, ZipOutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[20 * 1024];
        while (true) {
            int read = inputStream.read(buffer);
            if (read < 0) break;
            outputStream.write(buffer, 0, read);
        }
    }
}
