package Terminal;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class MyTerminal {
    private static String currentDirectory;
    private static List<String> commandHistory;

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            currentDirectory = System.getProperty("user.dir");
            commandHistory = new ArrayList<>();




            String command;

            while (true) {
                System.out.print(getPrompt());
                command = reader.readLine();

                if (command.equals("exit")) {
                    System.out.println("Exiting terminal...");
                    break;
                }

                if (command.trim().isEmpty()) {
                    // Empty line, print the same command line
                    continue;
                }



                commandHistory.add(command);



                executeCommand(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPrompt() {
        String user = System.getProperty("user.name");
        String hostname;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostname = localHost.getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }

        String promptDirectory;

        if (currentDirectory.equals(System.getProperty("user.home"))) {
            promptDirectory = "~"; // Wenn im Home-Verzeichnis, dann ~ anzeigen
        } else {
            promptDirectory = new File(currentDirectory).getAbsolutePath().replace("\\", "/"); // Andernfalls absoluten Pfad anzeigen
        }

        return String.format("%s@%s:%s$ ", user, hostname, promptDirectory);
    }



    private static void executeCommand(String command) throws IOException {
        String[] commandParts = command.split(" ");
        String commandName = commandParts[0];

        switch (commandName) {
            case "help":
                if (commandParts.length > 1) {
                    String helpCommand = commandParts[1];
                    showCommandHelp(helpCommand);
                } else {
                    showGeneralHelp();
                }
                break;
            case "clear":
                clearScreen();
                break;
            case "ls":
                if (commandParts.length > 1) {
                    String options = "";
                    List<String> directories = new ArrayList<>();

                    // Iteriere über die Befehlsteile ab dem zweiten Element
                    for (int i = 1; i < commandParts.length; i++) {
                        String part = commandParts[i];

                        if (part.startsWith("-")) {
                            // Parameter/Option gefunden
                            options += part.substring(1); // Füge Option zum String hinzu
                        } else {
                            // Verzeichnis gefunden
                            directories.add(part);
                        }
                    }

                    // Führe den ls-Befehl mit den angegebenen Parametern und Verzeichnissen aus
                    listFiles(options, directories.toArray(new String[0]));
                } else {
                    listFiles("", new String[0]); // Keine Optionen oder Verzeichnisse angegeben
                }
                break;
            case "cd":
                if (commandParts.length > 1) {
                    String newDirectory = commandParts[1];

                    if (newDirectory.startsWith("/")) {
                        // Absolute Pfadangabe
                        changeDirectory(newDirectory);
                    } else {
                        // Relative oder ~ Pfadangabe
                        if (newDirectory.equals("~")) {
                            newDirectory = System.getProperty("user.home");
                        }
                        changeDirectory(newDirectory);
                    }
                } else {
                    changeDirectory("");
                }
                break;
            case "mkdir":
                if (commandParts.length > 1) {
                    createDirectory(commandParts[1]);
                } else {
                    System.out.println("Missing argument for mkdir command.");
                }
                break;
            case "mkdirhier":
                if (commandParts.length > 1){
                    createDirectories(commandParts[1]);
                }else {
                    System.out.println("Missing argument for mkdirhier command.");
                }
                break;
            case "touch":
                if (commandParts.length > 1){
                    createFile(commandParts);
                } else {
                    System.out.println("Missing argument for touch command.");
                }
                break;
            case "rmdir":
                if (commandParts.length > 1) {
                    removeDirectory(commandParts[1]);
                } else {
                    System.out.println("Missing argument for rmdir command.");
                }
                break;
            case "rm":
                if (commandParts.length > 1){
                    removeFile(commandParts[1]);
                } else {
                    System.out.println("Missing argument for rm command.");
                }
                break;
            case "cp":
                if (commandParts.length > 2) {
                    if (commandParts[1].equals("-r")) {
                        copyDirectoryRecursively(commandParts[2], commandParts[3]);
                    } else {
                        copyFileOrDirectory(commandParts[1], commandParts[2]);
                    }
                } else {
                    System.out.println("Missing argument(s) for cp command.");
                }
                break;
            case "mv":
                if (commandParts.length > 2) {
                    moveFileOrDirectory(commandParts[1], commandParts[2]);
                } else {
                    System.out.println("Missing argument(s) for mv command.");
                }
                break;
            case "cat":
                if (commandParts.length > 1) {
                    displayFileContent(commandParts);
                } else {
                    System.out.println("Missing argument for cat command.");
                }
                break;
            case "pwd":
                printWorkingDirectory();
                break;
            case "tree":
                if (commandParts.length > 1) {
                    printDirectoryTree(new File(currentDirectory), Integer.parseInt(commandParts[1]));
                } else {
                    printDirectoryTree(new File(currentDirectory), 0);
                }
                break;
            case "ifconfig":
                showNetworkInterfaces();
                break;
            case "shutdown":
                if (commandParts.length > 1) {
                    String shutdownOption = commandParts[1];
                    if (shutdownOption.equals("now")) {
                        executeShutdownNow();
                    } else if (shutdownOption.matches("\\d{1,2}:\\d{2}")) {
                        String[] timeParts = shutdownOption.split(":");
                        int hours = Integer.parseInt(timeParts[0]);
                        int minutes = Integer.parseInt(timeParts[1]);
                        executeScheduledShutdown(hours, minutes);
                    } else if (shutdownOption.equals("-c")) {
                        executeCancelShutdown();
                    } else {
                        System.out.println("Invalid shutdown option: " + shutdownOption);
                    }
                } else {
                    System.out.println("Shutting down the system in 1 minute...");
                    executeShutdown();
                }
                break;
                //TODO: Funktioniert noch nicht
            /*case "zip":
                if (commandParts.length > 1) {
                    executeZip(commandParts);
                } else {
                    System.out.println("Missing argument for zip command.");
                }
                break;
            case "unzip":
                if (commandParts.length > 1) {
                    executeUnzip(commandParts);
                } else {
                    System.out.println("Missing argument for unzip command.");
                }
                break;*/
            case "echo":
                executeEcho(commandParts);
                break;
            case "ps":
                executePS();
                break;
            case "kill":
                if (commandParts.length > 1) {
                    String process = commandParts[1];
                    executeSystemCommand("taskkill /F /IM " + process);
                } else {
                    System.out.println("Missing argument for kill command.");
                }
                break;
            case "grep":
                if (commandParts.length > 3 && commandParts[1].equals("-c")) {
                    String pattern = commandParts[2];
                    String filePath = commandParts[3];

                    grepCountOccurrences(pattern, filePath);
                } else if (commandParts.length > 2 && commandParts[1].startsWith("\"") && commandParts[1].endsWith("\"")) {
                    String pattern = commandParts[1].substring(1, commandParts[1].length() - 1);
                    String filePath = "";

                    if (commandParts.length > 2) {
                        filePath = commandParts[2];
                    }

                    grepAndHighlight(pattern, filePath);
                } else {
                    System.out.println("Usage: grep [-c] <pattern> [<file>]");
                }
                break;
            case "wc":
                executeWc(commandParts);
                break;
            default:
                System.out.println("Command not found: " + commandName);
                break;
        }
    }

    //TODO: clear/cls hat Fehler
    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("bash", "-c", "clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error clearing the screen: " + e.getMessage());
        }
    }


    private static void listFiles(String options, String[] directories) {
        if (directories.length == 0) {
            directories = new String[]{currentDirectory};
        }

        for (String directory : directories) {
            File dir = new File(directory);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();

                if (files != null) {
                    if (options.contains("R")) {
                        listFilesRecursive(files, options);
                    } else if (options.contains("t")) {
                        listFilesByTime(files, options);
                    } else {
                        for (File file : files) {
                            String filename = file.getName();

                            // Check if the file should be displayed based on the options
                            boolean showFile = true;

                            if (!options.contains("a")) {
                                // Skip files starting with "."
                                if (filename.startsWith(".")) {
                                    showFile = false;
                                }
                            }

                            if (showFile) {
                                if (options.contains("l")) {
                                    String permissions = getPermissions(file);
                                    String size = getFileSize(file);
                                    String lastModified = getLastModified(file);
                                    System.out.printf("%s %s %s %s%n", permissions, size, lastModified, filename);
                                } else {
                                    System.out.println(filename);
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("Directory not found: " + directory);
            }
        }
    }

    private static void listFilesRecursive(File[] files, String options) {
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println(file.getAbsolutePath() + ":");
                File[] subFiles = file.listFiles();
                if (subFiles != null) {
                    listFilesRecursive(subFiles, options);
                }
            }

            String filename = file.getName();

            // Check if the file should be displayed based on the options
            boolean showFile = true;

            if (!options.contains("a")) {
                // Skip files starting with "."
                if (filename.startsWith(".")) {
                    showFile = false;
                }
            }

            if (showFile) {
                if (options.contains("l")) {
                    String permissions = getPermissions(file);
                    String size = getFileSize(file);
                    String lastModified = getLastModified(file);
                    System.out.printf("%s %s %s %s%n", permissions, size, lastModified, filename);
                } else {
                    System.out.println(filename);
                }
            }
        }
    }

    private static void listFilesByTime(File[] files, String options) {
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        for (File file : files) {
            String filename = file.getName();

            // Check if the file should be displayed based on the options
            boolean showFile = true;

            if (!options.contains("a")) {
                // Skip files starting with "."
                if (filename.startsWith(".")) {
                    showFile = false;
                }
            }

            if (showFile) {
                if (options.contains("l")) {
                    String permissions = getPermissions(file);
                    String size = getFileSize(file);
                    String lastModified = getLastModified(file);
                    System.out.printf("%s %s %s %s%n", permissions, size, lastModified, filename);
                } else {
                    System.out.println(filename);
                }
            }
        }
    }



    private static String getPermissions(File file) {
        StringBuilder permissions = new StringBuilder();

        // Owner permissions
        permissions.append(file.isDirectory() ? "d" : "-");
        permissions.append(file.canRead() ? "r" : "-");
        permissions.append(file.canWrite() ? "w" : "-");
        permissions.append(file.canExecute() ? "x" : "-");

        // Group permissions
        permissions.append(file.isDirectory() ? "r" : "-");
        permissions.append(file.isDirectory() ? "w" : "-");
        permissions.append(file.isDirectory() ? "x" : "-");

        // Other permissions
        permissions.append(file.isDirectory() ? "r" : "-");
        permissions.append(file.isDirectory() ? "w" : "-");
        permissions.append(file.isDirectory() ? "x" : "-");

        return permissions.toString();
    }

    private static String getFileSize(File file) {
        long bytes = file.length();
        String size = bytes + " B";

        if (bytes >= 1024) {
            double kilobytes = bytes / 1024.0;
            size = String.format("%.2f KB", kilobytes);

            if (kilobytes >= 1024) {
                double megabytes = kilobytes / 1024.0;
                size = String.format("%.2f MB", megabytes);

                if (megabytes >= 1024) {
                    double gigabytes = megabytes / 1024.0;
                    size = String.format("%.2f GB", gigabytes);
                }
            }
        }

        return size;
    }


    private static String getLastModified(File file) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(file.lastModified()));
    }


    private static void changeDirectory(String directory) {
        File newDirectory;

        if (directory.equals("..")) {
            // Wechsle ins übergeordnete Verzeichnis
            newDirectory = new File(currentDirectory).getParentFile();
        } else if (directory.equals("~")) {
            // Wechsle ins Home-Verzeichnis
            String homeDir = System.getProperty("user.home");
            newDirectory = new File(homeDir);
        } else if (directory.isEmpty()) {
            // Wenn kein Verzeichnis angegeben ist, wechsle ins Home-Verzeichnis des Benutzers
            String homeDir = System.getProperty("user.home");
            newDirectory = new File(homeDir);
        } else {
            newDirectory = new File(currentDirectory, directory);
        }

        if (!newDirectory.isAbsolute() && !directory.equals("~")) {
            // Wenn der angegebene Pfad kein absoluter Pfad ist, basiert er auf dem aktuellen Verzeichnis
            newDirectory = new File(currentDirectory, directory);
        }

        if (newDirectory.exists() && newDirectory.isDirectory()) {
            if (!directory.equals("~")) {
                currentDirectory = newDirectory.getAbsolutePath();
            } else {
                currentDirectory = "~";
            }
        } else {
            System.out.println("Directory not found: " + directory);
        }
    }











    private static void createDirectory(String directory) {
        if (directory.isEmpty()) {
            System.out.println("Missing directory name.");
            return;
        }

        String[] directoryNames = directory.split(",");

        for (String dirName : directoryNames) {
            File newDirectory = new File(currentDirectory, dirName.trim());

            if (newDirectory.exists()) {
                System.out.println("Directory already exists: " + newDirectory.getAbsolutePath());
                continue;
            }

            if (newDirectory.mkdirs()) {
                System.out.println("Directory created: " + newDirectory.getAbsolutePath());
            } else {
                System.out.println("Failed to create directory: " + dirName);
            }
        }
    }


    private static void createDirectories(String directory) {
        File newDirectory = new File(currentDirectory, directory);

        if (newDirectory.mkdirs()) {
            System.out.println("Directory created: " + newDirectory.getAbsolutePath());
        } else {
            System.out.println("Failed to create directory: " + directory);
        }
    }


    private static void createFile(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: touch <file>");
            return;
        }

        String fileName = commandParts[1];
        String filePath = currentDirectory + File.separator + fileName;

        File file = new File(filePath);
        try {
            if (file.createNewFile()) {
                System.out.println("File created: " + filePath);
            } else {
                System.out.println("Failed to create file: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("Failed to create file: " + e.getMessage());
        }
    }

    private static void removeDirectory(String directory) throws IOException {
        Path dirPath = Paths.get(currentDirectory.toString(), directory);

        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try {
                Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                System.out.println("Directory " + directory + " deleted.");
            } catch (IOException e) {
                System.out.println("Failed to delete directory: " + e.getMessage());
            }
        } else {
            System.out.println("Directory not found: " + directory);
        }
    }

    private static void removeFile(String filePath) {
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(currentDirectory, filePath); // Combine with the current directory if it's a relative path
        }

        if (file.exists()) {
            if (file.delete()) {
                System.out.println("File deleted: " + file.getAbsolutePath());
            } else {
                System.out.println("Failed to delete file: " + file.getAbsolutePath());
            }
        } else {
            System.out.println("File not found: " + file.getAbsolutePath());
        }
    }


    private static void copyFileOrDirectory(String source, String destination) {
        File sourceFile = new File(currentDirectory, source);
        File destinationFile = new File(currentDirectory, destination);

        if (sourceFile.exists()) {
            if (sourceFile.isDirectory()) {
                copyDirectory(sourceFile, destinationFile);
            } else {
                try {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath());
                    System.out.println("File copied: " + sourceFile.getAbsolutePath() + " -> " + destinationFile.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Failed to copy file: " + sourceFile.getName());
                }
            }
        } else {
            System.out.println("Source file or directory not found: " + source);
        }
    }

    private static void copyDirectoryRecursively(String source, String destination) {
        File sourceDirectory = new File(currentDirectory, source);
        File destinationDirectory = new File(currentDirectory, destination);

        if (sourceDirectory.exists()) {
            if (sourceDirectory.isDirectory()) {
                copyDirectory(sourceDirectory, destinationDirectory);
                System.out.println("Directory copied recursively: " + sourceDirectory.getAbsolutePath() + " -> " + destinationDirectory.getAbsolutePath());
            } else {
                System.out.println("Source is not a directory: " + sourceDirectory.getName());
            }
        } else {
            System.out.println("Source directory not found: " + source);
        }
    }


    private static void copyDirectory(File sourceDirectory, File destinationDirectory) {
        if (!destinationDirectory.exists()) {
            if (destinationDirectory.mkdir()) {
                System.out.println("Directory created: " + destinationDirectory.getAbsolutePath());
            } else {
                System.out.println("Failed to create directory: " + destinationDirectory.getName());
                return;
            }
        }

        File[] files = sourceDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                File destinationFile = new File(destinationDirectory, file.getName());

                if (file.isDirectory()) {
                    copyDirectory(file, destinationFile);
                } else {
                    try {
                        Files.copy(file.toPath(), destinationFile.toPath());
                        System.out.println("File copied: " + file.getAbsolutePath() + " -> " + destinationFile.getAbsolutePath());
                    } catch (IOException e) {
                        System.out.println("Failed to copy file: " + file.getName());
                    }
                }
            }
        }
    }

    private static void moveFileOrDirectory(String source, String destination) {
        File sourceFile = new File(currentDirectory, source);
        File destinationFile = new File(currentDirectory, destination);

        if (sourceFile.exists()) {
            if (sourceFile.renameTo(destinationFile)) {
                System.out.println("File or directory moved/renamed: " + sourceFile.getAbsolutePath() + " -> " + destinationFile.getAbsolutePath());
            } else {
                System.out.println("Failed to move/rename file or directory: " + sourceFile.getName());
            }
        } else {
            System.out.println("Source file or directory not found: " + source);
        }
    }

    private static void displayFileContent(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: cat <file>");
            return;
        }

        String fileName = commandParts[1];
        String filePath = currentDirectory + File.separator + fileName;

        File file = new File(filePath);
        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Failed to read file: " + e.getMessage());
            }
        } else {
            System.out.println("File not found: " + filePath);
        }
    }

    private static void printWorkingDirectory() {
        System.out.println(currentDirectory);
    }

    public static void printDirectoryTree(File directory, int depth) {
        if (directory.isDirectory()) {
            // Ausgabe des aktuellen Verzeichnisses
            System.out.println(getIndentation(depth) + directory.getName() + "/");

            // Rekursiver Aufruf für die Unterverzeichnisse
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    printDirectoryTree(file, depth + 1);
                }
            }
        } else {
            // Ausgabe der Datei
            System.out.println(getIndentation(depth) + directory.getName());
        }
    }

    public static String getIndentation(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("  "); // Leerzeichen zur Einrückung
        }
        return sb.toString();
    }


    private static void showNetworkInterfaces() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            System.out.println("Interface: " + networkInterface.getName());

            List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
            for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                InetAddress address = interfaceAddress.getAddress();
                System.out.println("  IP Address: " + address.getHostAddress());
                System.out.println("  Subnet Mask: " + calculateSubnetMask(interfaceAddress.getNetworkPrefixLength()));
                System.out.println();
            }
        }
    }

    private static String calculateSubnetMask(short prefixLength) {
        int subnetMask = 0xffffffff << (32 - prefixLength);
        return (subnetMask >>> 24) + "." + ((subnetMask >> 16) & 0xff) + "." + ((subnetMask >> 8) & 0xff) + "." + (subnetMask & 0xff);
    }

    private static void executeShutdown() throws IOException {
        //Perform shutdown in 1 minute
        // For Linux/Unix-based systems
        //String shutdownCommand = "shutdown +1"; // Herunterfahren in 1 Minute
        //Runtime.getRuntime().exec(new String[] { "bash", "-c", shutdownCommand });
        //System.out.println("Computer wird in 1 Minute heruntergefahren.");
        // For Windows systems
        String shutdownCommand = "shutdown"; // Herunterfahren in 1 Minute
        Runtime.getRuntime().exec(shutdownCommand);
        System.out.println("Computer wird in 1 Minute heruntergefahren.");
    }

    private static void executeShutdownNow() throws IOException {
        // Perform immediate shutdown, based on the operating system
        // For Linux/Unix-based systems
        // String shutdownCommand = "shutdown -h now";
        // Runtime.getRuntime().exec(new String[] { "bash", "-c", shutdownCommand });
        // System.out.println("Computer wird sofort heruntergefahren und ausgeschaltet.");

        // For Windows systems
        String shutdownCommand = "shutdown -s -f -t 0";
        Runtime.getRuntime().exec(shutdownCommand);
        System.out.println("Computer wird sofort heruntergefahren und ausgeschaltet.");
    }

    private static void executeScheduledShutdown(int hours, int minutes) throws IOException {
        // Perform shutdown at the specified time, based on the operating system
        // For Linux/Unix-based systems
        // String shutdownCommand = "shutdown -h " + hours + ":" + minutes;
        // Runtime.getRuntime().exec(new String[] { "bash", "-c", shutdownCommand });
        // System.out.println("Computer wird um " + hours + ":" + minutes + " heruntergefahren.");

        // For Windows systems
        String shutdownCommand = "shutdown /s /f /t " + (hours * 3600 + minutes * 60);
        Runtime.getRuntime().exec(shutdownCommand);
        System.out.println("Computer wird in " + hours + " Stunden und " + minutes + " Minuten heruntergefahren.");
    }

    private static void executeCancelShutdown() throws IOException {
        // Cancel a scheduled shutdown, based on the operating system
        // For Linux/Unix-based systems
        // String cancelCommand = "shutdown -c";
        // Runtime.getRuntime().exec(new String[] { "bash", "-c", cancelCommand });
        // System.out.println("Geplanter Herunterfahrungsprozess wurde abgebrochen.");

        // For Windows systems
        String cancelCommand = "shutdown /a";
        Runtime.getRuntime().exec(cancelCommand);
        System.out.println("Geplanter Herunterfahrungsprozess wurde abgebrochen.");
    }



    //TODO: executeZip(), executeUnzip() Funktioniert noch nicht
    /*

    private static void executeZip(String[] commandParts) throws IOException {
        if (commandParts.length > 1) {
            String sourcePath = commandParts[1];
            String destZipFile = commandParts.length > 2 ? commandParts[2] : "";

            String zipCommand = "zip";
            if (sourcePath.endsWith(".rar") || sourcePath.endsWith(".7z")) {
                zipCommand = "7z";
            }

            // Hier den Pfad zu deinem zip-Programm angeben
            String zipExecutablePath = "C:/Program Files/WinRAR/WinRAR.exe";

            ProcessBuilder processBuilder = new ProcessBuilder(zipExecutablePath, destZipFile, sourcePath);
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();
                int exitCode;
                try {
                    exitCode = process.waitFor();
                } catch (InterruptedException e) {
                    throw new IOException("Failed to wait for process completion.", e);
                }

                if (exitCode == 0) {
                    System.out.println("Compression successful.");
                } else {
                    System.out.println("Compression failed with exit code: " + exitCode);
                }
            } catch (IOException e) {
                System.out.println("Error executing zip command: " + e.getMessage());
            }
        } else {
            System.out.println("Missing argument for zip command.");
        }
    }





    private static void executeUnzip(String[] commandParts) throws IOException {
        if (commandParts.length > 1) {
            String filePath = commandParts[1];
            String destDirectory = commandParts.length > 2 ? commandParts[2] : "";

            ProcessBuilder processBuilder = new ProcessBuilder("unzip", filePath, "-d", destDirectory);
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();
                int exitCode;
                try {
                    exitCode = process.waitFor();
                } catch (InterruptedException e) {
                    throw new IOException("Failed to wait for process completion.", e);
                }

                if (exitCode == 0) {
                    System.out.println("Extraction successful.");
                } else {
                    System.out.println("Extraction failed with exit code: " + exitCode);
                }
            } catch (IOException e) {
                System.out.println("Error executing unzip command: " + e.getMessage());
            }
        } else {
            System.out.println("Missing argument for unzip command.");
        }
    }

    */



    private static void executeEcho(String[] commandParts) {
        if (commandParts.length > 1) {
            StringBuilder message = new StringBuilder();

            for (int i = 1; i < commandParts.length; i++) {
                message.append(commandParts[i]).append(" ");
            }

            String outputMessage = message.toString().trim();

            // Ersetze $USER durch den tatsächlichen Benutzernamen
            if (outputMessage.contains("$USER")) {
                String currentUser = System.getProperty("user.name");
                outputMessage = outputMessage.replace("$USER", currentUser);
            }

            // Überprüfe, ob die Nachricht mehrere Worte enthält und nicht von Anführungszeichen umgeben ist
            if (outputMessage.contains(" ") && !outputMessage.startsWith("\"")) {
                System.out.println("Use quotes for messages with multiple words.");
            } else {
                // Entferne Anführungszeichen aus der Ausgabe, falls vorhanden
                if (outputMessage.startsWith("\"") && outputMessage.endsWith("\"")) {
                    outputMessage = outputMessage.substring(1, outputMessage.length() - 1);
                }
                System.out.println(outputMessage);
            }
        } else {
            System.out.println("Missing argument for echo command.");
        }
    }








    private static void executePS() {
        try {
            // Erstelle einen Prozessbuilder für den tasklist-Befehl
            ProcessBuilder processBuilder = new ProcessBuilder("tasklist");
            processBuilder.redirectErrorStream(true); // Leite den Fehlerstrom in den Ausgabestrom um

            // Starte den Prozess und hole seinen Ausgabestrom
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();

            // Lese und zeige den Ausgabestrom zeilenweise
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "CP437")); // Windows-1252 funktioniert auch
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Warte auf den Prozess, bis er beendet ist
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }



    private static void executeKill(String[] commandParts) throws IOException {
        if (commandParts.length > 1) {
            String process = commandParts[1];
            try {
                if (Character.isDigit(process.charAt(0))) {
                    int pid = Integer.parseInt(process);
                    System.out.println("Killing process with PID: " + pid);
                    executeSystemCommand("taskkill /F /PID " + pid);
                } else {
                    System.out.println("Killing process: " + process);
                    executeSystemCommand("taskkill /F /IM " + process);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid PID.");
            }
        } else {
            System.out.println("Missing argument for kill command.");
        }
    }

    private static void executeSystemCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
            reader.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static void grepAndHighlight(String pattern, String filePath) {
        File file = new File(currentDirectory, filePath);

        if (file.exists() && file.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.contains(pattern)) {
                        String highlightedLine = line.replaceAll(pattern, "\u001B[31m" + pattern + "\u001B[0m");
                        System.out.println(highlightedLine);
                    } else {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("File not found: " + filePath);
        }
    }

    //TODO: gibt nur "Usage: grep [-c] <pattern> [<file>]" aus

    private static void grepCountOccurrences(String pattern, String filePath) {
        File file = new File(currentDirectory, filePath);

        if (file.exists() && file.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int occurrences = 0;

                while ((line = reader.readLine()) != null) {
                    occurrences += countOccurrences(line, pattern);
                }

                System.out.println("Pattern \"" + pattern + "\" found " + occurrences + " time(s).");
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("File not found: " + filePath);
        }
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = text.indexOf(pattern);

        while (index != -1) {
            count++;
            index = text.indexOf(pattern, index + 1);
        }

        return count;
    }

    private static void executeWc(String[] commandParts) {
        if (commandParts.length > 1) {
            boolean countWordsOnly = false;
            boolean countLines = true;
            boolean countBytes = true;
            String filePath = commandParts[1];

            if (commandParts.length > 2) {
                if (commandParts[1].equals("-w")) {
                    countWordsOnly = true;
                    filePath = commandParts[2];
                } else if (commandParts[1].equals("-l")) {
                    countWordsOnly = false;
                    countBytes = false;
                    filePath = commandParts[2];
                } else if (commandParts[1].equals("-c")) {
                    countWordsOnly = false;
                    countLines = false;
                    filePath = commandParts[2];
                }
            }

            File file = new File(currentDirectory, filePath);

            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    int lines = 0;
                    int words = 0;
                    int bytes = 0;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (countLines) {
                            lines++;
                        }
                        if (countBytes) {
                            bytes += line.getBytes().length;
                        }
                        if (!countWordsOnly) {
                            String[] wordsArray = line.split("\\s+");
                            words += wordsArray.length;
                        }
                    }

                    if (countWordsOnly) {
                        System.out.println(words + " " + filePath);
                    } else {
                        String result = "";
                        if (countLines) {
                            result += lines + " ";
                        }
                        if (!countWordsOnly) {
                            result += words + " ";
                        }
                        if (countBytes) {
                            result += bytes + " ";
                        }
                        System.out.println(result + filePath);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + e.getMessage());
                }
            } else {
                System.out.println("File not found: " + filePath);
            }
        } else {
            System.out.println("Missing argument for wc command.");
        }
    }








    private static void showGeneralHelp() {
        System.out.println("Available commands:");
        System.out.println("  help <command>             - Show help for a specific command");
        System.out.println("  clear                      - Clear the terminal screen");
        System.out.println("  ls [options] [directory]   - List files and directories");
        System.out.println("  ls -l [directory]          - List files and directories in long format");
        System.out.println("  ls -a [directory]          - List all files and directories, including hidden ones");
        System.out.println("  ls -R [directory]          - List files and directories recursively");
        System.out.println("  ls -t [directory]          - List files and directories, sorted by modification time");
        System.out.println("  cd [directory]             - Change current directory");
        System.out.println("  mkdir [directory]          - Create a new directory");
        System.out.println("  mkdirhier [directory]      - Create directories hierarchically");
        System.out.println("  touch [file]               - Create a file");
        System.out.println("  rmdir [directory]          - Remove a directory");
        System.out.println("  rm [file]                  - Remove a file");
        System.out.println("  cp [source] [dest]         - Copy a file or directory");
        System.out.println("  cp -r [source] [dest]      - Copy directories recursively");
        System.out.println("  mv [source] [dest]         - Move/rename a file or directory");
        System.out.println("  cat [file]                 - Display the content of a file");
        System.out.println("  pwd                        - Print the current working directory");
        System.out.println("  tree                       - Print the whole directory hierarchy");
        System.out.println("  ifconfig                   - Display network interface information");
        System.out.println("  shutdown [options]         - Shutdown, halt, or restart the system");
        System.out.println("  zip [file]                 - Compress files or directories into a zip archive");
        System.out.println("  unzip [file]               - Extract the contents of a zip archive");
        System.out.println("  echo <message>             - Display a message");
        System.out.println("  ps                         - View running processes");
        System.out.println("  kill <pid|process>         - Terminate a process");
        System.out.println("  grep <pattern> <file>      - Search for a pattern in a file and display matching lines");
        System.out.println("  grep -c <pattern> <file>   - Count occurrences of a pattern in a file");
        System.out.println("  wc [options] file          - Count lines, words, and bytes in a file");
        System.out.println("  wc -w                      - Count the number of words");
        System.out.println("  wc -l                      - Count the number of lines");
        System.out.println("  wc -c                      - Count the number of bytes");
    }



    private static void showCommandHelp(String command) {
        switch (command) {
            case "help":
                System.out.println("Usage: help <command>");
                System.out.println("Show help for a specific command.");
                break;
            case "clear":
                System.out.println("Usage: clear");
                System.out.println("Clear the terminal screen.");
                break;
            case "ls":
                System.out.println("Usage: ls [options] [directory]");
                System.out.println("List files and directories.");
                System.out.println("Options:");
                System.out.println("  -l - List files and directories in long format");
                System.out.println("  -a - List all files and directories, including hidden ones");
                System.out.println("  -R - List files and directories recursively");
                System.out.println("  -t - List files and directories, sorted by modification time");
                break;
            case "cd":
                System.out.println("Usage: cd [directory]");
                System.out.println("Change the current directory.");
                break;
            case "mkdir":
                System.out.println("Usage: mkdir [directory]");
                System.out.println("Create a new directory.");
                break;
            case "mkdirhier":
                System.out.println("Usage: mkdirhier [directory]");
                System.out.println("Create directories hierarchically.");
                break;
            case "touch":
                System.out.println("Usage: touch [file]");
                System.out.println("Create a file.");
                break;
            case "rmdir":
                System.out.println("Usage: rmdir [directory]");
                System.out.println("Remove a directory.");
                break;
            case "rm":
                System.out.println("Usage: rm [file]");
                System.out.println("Remove a file.");
                break;
            case "cp":
                System.out.println("Usage: cp [source] [dest]");
                System.out.println("Copy a file or directory.");
                System.out.println("Options:");
                System.out.println("  -r - Copy directories recursively");
                break;
            case "mv":
                System.out.println("Usage: mv [source] [dest]");
                System.out.println("Move/rename a file or directory.");
                break;
            case "cat":
                System.out.println("Usage: cat [file]");
                System.out.println("Display the content of a file.");
                break;
            case "pwd":
                System.out.println("Usage: pwd");
                System.out.println("Print the current working directory.");
                break;
            case "tree":
                System.out.println("Usage: tree");
                System.out.println("Print the whole directory hierarchy.");
                break;
            case "ifconfig":
                System.out.println("Usage: ifconfig");
                System.out.println("Display network interface information.");
                break;
            case "shutdown":
                System.out.println("Usage: shutdown [options]");
                System.out.println("Shutdown, halt, or restart the system.");
                System.out.println("Options:");
                System.out.println("  now - Shutdown the system immediately");
                System.out.println("  <time> - Shutdown the system at the specified time (24-hour format)");
                System.out.println("Options:");
                System.out.println("  -c - Cancel a previously scheduled shutdown");
                break;
            case "zip":
                System.out.println("Usage: zip [file]");
                System.out.println("Compress files or directories into a zip, rar, or 7z archive.");
                System.out.println("Supported archive formats: .zip, .rar, .7z");
                break;
            case "unzip":
                System.out.println("Usage: unzip [file]");
                System.out.println("Extract the contents of a zip, rar, or 7z archive.");
                System.out.println("Supported archive formats: .zip, .rar, .7z");
                break;
            case "echo":
                System.out.println("Usage: echo <message>");
                System.out.println("Display a message.");
                break;
            case "ps":
                System.out.println("Usage: ps");
                System.out.println("View running processes.");
                break;
            case "kill":
                System.out.println("Usage: kill <pid|process>");
                System.out.println("Terminate a process.");
                System.out.println("You can use either the PID (Process ID) or the binary name of the program.");
                break;
            case "grep":
                System.out.println("Usage: grep <pattern> <file>");
                System.out.println("Search for a pattern in a file and display matching lines.");
                System.out.println("Usage: grep -c <pattern> <file>");
                System.out.println("Count occurrences of a pattern in a file.");
                break;
            case "wc":
                System.out.println("Usage: wc [options] file");
                System.out.println("Count the number of lines, words, and bytes in a file.");
                System.out.println("Options:");
                System.out.println("  -w - Count the number of words");
                System.out.println("  -l - Count the number of lines");
                System.out.println("  -c - Count the number of bytes");
                break;
            default:
                System.out.println("Command not found: " + command);
                break;
        }
    }


}

