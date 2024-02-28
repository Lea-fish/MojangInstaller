package de.leafish;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public final class Installer {

    // https://bugs.mojang.com/browse/MCL-23639

    // FIXME: support updating installer

    private static final String INSTALLER_PATH = "installer";

    public static void main(String[] args) {
        OperatingSystem os = detectOperatingSystem();
        ArrayList<String> command = new ArrayList<>(Arrays.asList(args));
        try {
            File dir = Files.createTempDirectory("leafish_install").toFile();
            dir.mkdir();
            dir.deleteOnExit();
            command.add(0, dir.getAbsolutePath() + "/" + INSTALLER_PATH + getExecutableExtension(os));
            // try starting the bootstrap twice as it might have downloaded an update the first time it was started,
            // so we are always running the latest bootstrap available
            startInstaller(command, dir);
            // command.add("noupdate");
            // startInstaller(command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void startInstaller(ArrayList<String> command, File directory) throws Exception {
        OperatingSystem os = detectOperatingSystem();
        File out = new File(directory.getAbsolutePath() + "/" + INSTALLER_PATH + getExecutableExtension(os));
        // File updated = new File("./" + UPDATE_PATH + getExecutableExtension(os));
        System.out.println("Checking for installer...");
        if (!out.exists()) {
            System.out.println("Extracting installer...");
            String fileSuffix = getProcessorArchitecture().name().toLowerCase() + "_" + os.name().toLowerCase() + getExecutableExtension(os);
            InputStream stream = Installer.class.getResourceAsStream("/install_" + fileSuffix);
            if (stream == null) {
                throw new RuntimeException("Failed extracting bootstrap binary from wrapper jar, is your architecture and operating system supported?");
            }
            java.nio.file.Files.copy(
                    stream,
                    out.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Adjusting perms...");
            adjustPerms(directory);
        }/* else if (updated.exists()) {
            Files.copy(updated.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            updated.delete();
            adjustPerms(); // FIXME: is this needed?
        }*/

        Process proc = new ProcessBuilder(command).directory(directory).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        proc.waitFor();
    }

    private static void adjustPerms(File dir) throws Exception {
        // FIXME: does MAC also need perms?
        OperatingSystem os = detectOperatingSystem();
        if (os == OperatingSystem.LINUX) {
            // try giving us execute perms
            new ProcessBuilder(("chmod 777 " + INSTALLER_PATH + getExecutableExtension(os)).split(" ")).redirectOutput(ProcessBuilder.Redirect.INHERIT).directory(dir).redirectError(ProcessBuilder.Redirect.INHERIT).start().waitFor();
        }
    }

    private enum OperatingSystem {
        WINDOWS, LINUX, MACOS, UNKNOWN
    }

    private static OperatingSystem detectOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        // Return the corresponding enum based on the operating system
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("nux")) {
            return OperatingSystem.LINUX;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MACOS;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    private enum Architecture {
        X86(32),
        X86_64(64),
        ARM(32),
        AARCH64(64),
        UNKNOWN(0);

        private final int bitSize;

        Architecture(int bitSize) {
            this.bitSize = bitSize;
        }

        public int getBitSize() {
            return bitSize;
        }
    }

    private static Architecture getProcessorArchitecture() {
        String osArch = System.getProperty("os.arch").toLowerCase();

        if (osArch.contains("x86_64") || osArch.contains("amd64")) {
            return Architecture.X86_64;
        } else if (osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i486") || osArch.contains("i586") || osArch.contains("i686")) {
            return Architecture.X86;
        } else if (osArch.contains("aarch64")) {
            return Architecture.AARCH64;
        } else if (osArch.contains("arm")) {
            return Architecture.ARM;
        } else {
            return Architecture.UNKNOWN;
        }
    }

    private static String getExecutableExtension(OperatingSystem os) {
        if (os == OperatingSystem.WINDOWS) {
            return ".exe";
        } else {
            return "";
        }
    }

}
