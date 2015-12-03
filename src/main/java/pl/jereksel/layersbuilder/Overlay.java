package pl.jereksel.layersbuilder;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class Overlay {

    private File overlayJsonFile;
    private File pluginJsonFile;

    public Overlay(File overlayJsonFile, File pluginJsonFile) {
        this.overlayJsonFile = overlayJsonFile;
        this.pluginJsonFile = pluginJsonFile;
    }

    public String getLocation() {
        return overlayJsonFile.getParent();
    }

    public String getType() {
        try {
            return new JSONObject(FileUtils.readFileToString(overlayJsonFile)).getString("overlay_type");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getColor() {
        try {
            return new JSONObject(FileUtils.readFileToString(overlayJsonFile)).getString("color");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getStyle() {
        try {
            return new JSONObject(FileUtils.readFileToString(overlayJsonFile)).getString("style");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getName() {
        try {
            return new JSONObject(FileUtils.readFileToString(overlayJsonFile)).getString("name");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File build(boolean alone) throws Exception {

        File jsonFile = overlayJsonFile;

        File tempDir = Files.createTempDir();

        if (alone) {
            System.out.println("Temp dir: " + tempDir);
        }

        //Copy everything to temp folder
        //FalseFileFilter, because we don't want to copy json
        Collection<File> files = FileUtils.listFilesAndDirs(jsonFile.getParentFile(), FalseFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        files.remove(jsonFile.getParentFile());

        File resTemp = new File(tempDir.getAbsolutePath() + File.separator + "resTemp");

        File androidManifestLocation = new File(tempDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");

        for (File file : files) {
            FileUtils.copyDirectoryToDirectory(file, resTemp);
        }

        JSONObject config = new JSONObject(FileUtils.readFileToString(jsonFile));

        String manifest = IOUtils.toString(Main.class.getClassLoader().getResourceAsStream("Manifest.xml"));

        String targetPackage = config.getString("target");
        JSONObject pluginConfig = new JSONObject(FileUtils.readFileToString(pluginJsonFile));

        manifest = manifest
                .replace("<<PRIORITY>>", config.getString("priority"))
                .replace("<<PACKAGE_NAME>>", pluginConfig.getString("package_name") + ".layer." + targetPackage)
                .replace("<<TARGET>>", targetPackage);

        FileUtils.writeStringToFile(androidManifestLocation, manifest);

        FileUtils.writeStringToFile(new File(tempDir.getAbsolutePath() + File.separator + "lint.xml"),
                IOUtils.toString(Main.class.getClassLoader().getResourceAsStream("lint.xml"))
        );

        File target = new File(tempDir.getAbsolutePath() + File.separator + (pluginConfig.getString("plugin_name").replace(" ", "") + "_" + config.getString("name").replace(" ", "_")) + ".apk");

        if (getType().equals("custom")) {
            target = new File(target.getAbsolutePath().replace(".apk", "") + "_" + getStyle() + ".apk");
        }

        //Compile

        System.out.println("Compiling");

        Process compile = new ProcessBuilder().command("aapt", "p", "-M", androidManifestLocation.getAbsolutePath(),
                "-S", resTemp.getAbsolutePath(), "-I", pluginConfig.getString("framework_location"), "-F",
                target.getAbsolutePath()).start();

        InputStream inputStream = compile.getInputStream();
        InputStream errorStream = compile.getErrorStream();

        /*String inputCompile = IOUtils.toString(inputStream);*/
        String errorCompile = IOUtils.toString(errorStream);

        compile.waitFor();

        if (!StringUtils.isEmpty(errorCompile)) {
            System.out.println("Errors: " + errorCompile);
        }

        //Signing

        System.out.println("Signing");


        String dataFolder = System.getProperty("user.home");

        File debugKey = new File(dataFolder + File.separator + ".android" + File.separator + "debug.keystore");

        Process sign = new ProcessBuilder().command("jarsigner", "-keystore",
                debugKey.getAbsolutePath(), "-storepass", "android", "-keypass", "android",
                target.getAbsolutePath(), "androiddebugkey").start();

        inputStream = compile.getInputStream();
        errorStream = compile.getErrorStream();

        /*String inputSign = IOUtils.toString(inputStream);*/
        String errorSign = IOUtils.toString(errorStream);

        sign.waitFor();

        if (!StringUtils.isEmpty(errorSign)) {
            System.out.println("Errors: " + errorSign);
        }

        //Installing
        boolean autoInstall = false;
        try {
            autoInstall = config.getBoolean("auto_install");
        } catch (Exception e) {
            autoInstall = false;
        }

        if (alone) {

            if (autoInstall) {

                System.out.println("Installing");


                Process installing = new ProcessBuilder().command("adb", "install", "-r", target.getAbsolutePath()).start();

                inputStream = installing.getInputStream();
                errorStream = installing.getErrorStream();

                String inputInstall = IOUtils.toString(inputStream);
                String errorInstall = IOUtils.toString(errorStream);

                installing.waitFor();

                if (!StringUtils.isEmpty(errorInstall)) {
                    System.out.print("Output: " + inputInstall);
                    System.out.print("Possible error: " + errorInstall);
                }

            } else {

                //Push to /system/vendor/overlay

                Process process1 = new ProcessBuilder().command("adb", "root").start();

                inputStream = process1.getInputStream();
                errorStream = process1.getErrorStream();

                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(errorStream);

                process1.waitFor();

                process1 = new ProcessBuilder().command("adb", "remount").start();

                inputStream = process1.getInputStream();
                errorStream = process1.getErrorStream();

                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(errorStream);

                process1.waitFor();

                process1 = new ProcessBuilder().command("adb", "push", target.getAbsolutePath(), "/system/vendor/overlay/").start();

                inputStream = process1.getInputStream();
                errorStream = process1.getErrorStream();

                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(errorStream);

                process1.waitFor();


                process1 = new ProcessBuilder().command("adb", "shell", "chmod", "644", "/system/vendor/overlay/" + FilenameUtils.getName(target.getAbsolutePath())).start();

                inputStream = process1.getInputStream();
                errorStream = process1.getErrorStream();

                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(errorStream);

                process1.waitFor();

                process1 = new ProcessBuilder().command("adb", "remount").start();

                inputStream = process1.getInputStream();
                errorStream = process1.getErrorStream();

                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(errorStream);

                process1.waitFor();


                System.out.println("Apk location: " + target.getAbsolutePath());
            }

        }

        return target;

    }
}
