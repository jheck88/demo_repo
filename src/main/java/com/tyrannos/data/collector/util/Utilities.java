package com.tyrannos.data.collector.util;

public class Utilities {

    private static final String MAC_OS = "mac";
    private static final String WIN_OS = "win";
    private static final String UNIX_OS = "nux";

    private static final String PROPERTY_OS = "os.name";

    private static String operatingSystem = null;

    private static final Utilities instance = new Utilities();

    public static Utilities getInstance() {
        return instance;
    }

    public boolean isWindows() {
        getOsName();
        return (operatingSystem.indexOf(WIN_OS) >= 0);
    }

    public boolean isMac() {
        getOsName();
        return (operatingSystem.indexOf(MAC_OS) >= 0);
    }


    public boolean isUnix() {
        getOsName();
        return (operatingSystem.indexOf(UNIX_OS) >= 0);
    }

    public String getOsName() {
        if (operatingSystem == null) {
            operatingSystem = System.getProperty(PROPERTY_OS).toLowerCase();
        }
        return operatingSystem;
    }
}