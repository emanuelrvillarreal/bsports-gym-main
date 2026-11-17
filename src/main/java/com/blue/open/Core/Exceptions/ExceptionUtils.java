package com.blue.open.Core.Exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    public static String buildDeveloperMessage(String context, Exception ex) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        ex.printStackTrace(printWriter);
        printWriter.flush();
        return context + " | " + ex.getClass().getName() + ": " + ex.getMessage() + System.lineSeparator()
                + stringWriter;
    }
}
