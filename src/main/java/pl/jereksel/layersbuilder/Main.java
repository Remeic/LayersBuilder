package pl.jereksel.layersbuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Main {

    public static void main(String[] args)
            throws Exception {

        if (args.length == 0 || args.length > 2) {
        	System.out.println("cdt's Internal LayersBuilder 2.0 - Windows");
        	System.out.println("Original library by Andrzej Ressel (jereksel)\n");
            System.out.println("Usage: <plugin config> <overlay config (optional)>");
            System.exit(-1);
        }

        /*File pluginFile;
        File overlayFile;*/

        List<Overlay> allOverlays = new ArrayList<>();
        List<Overlay> generalOverlays = new ArrayList<>();
        ArrayListMultimap<String, Overlay> colorOverlays = ArrayListMultimap.create();
        ArrayListMultimap<String, Overlay> customStyleOverlays = ArrayListMultimap.create();


        if (args.length == 1) {

            JSONObject pluginData = new JSONObject(FileUtils.readFileToString(new File(args[0])));

            JSONArray locations = pluginData.getJSONArray("overlays");


            for (Object locationO : Lists.newArrayList(locations.iterator())) {

                String location = new File(args[0]).getAbsoluteFile().getParent() + File.separator + (String) locationO;

                //It's another folder
                if (!new File(location + File.separator + "config.json").exists()) {

                    Collection<File> overlayFolders = FileUtils.listFilesAndDirs(new File(location), FalseFileFilter.INSTANCE, new OneLevelFileFilter(new File(location)));

                    overlayFolders.remove(new File(location));

                    for (File overlayFolder : overlayFolders) {

                        File configFile = new File(overlayFolder + File.separator + "config.json");

                        if (!configFile.exists()) {
                            System.out.println("[WARNING] config doesn't exist in " + overlayFolder);
                        } else {
                            allOverlays.add(new Overlay(configFile, new File(args[0])));
                        }


                    }

                } else {
                    allOverlays.add(new Overlay(new File(location + File.separator + "config.json"), new File(args[0])));
                }


            }


            for (Overlay overlay : allOverlays) {

                String type = overlay.getType();

                if (type.equals("general")) {
                    generalOverlays.add(overlay);
                } else if (type.equals("color")) {
                    colorOverlays.put(overlay.getColor(), overlay);
                } else if (type.equals("custom")) {
                    customStyleOverlays.put(overlay.getName(), overlay);
                } else {
                    System.out.println("[ERROR] Unknown type " + type + "in " + overlay.getLocation());
                }


            }

            File tempDir = Files.createTempDir();

            /*System.out.println("Temp dir: " + tempDir);*/
        	System.out.println("cdt's Internal LayersBuilder 2.0 - Windows ONLY");
        	System.out.println("Original library by Andrzej Ressel (jereksel)\n");

            File generalFolder = new File(tempDir.getAbsolutePath() + File.separator + "General");


            //Let's compile everything


            //General overlays

            for (Overlay overlay : generalOverlays) {
                File file = overlay.build(false);
                FileUtils.moveFileToDirectory(file, generalFolder, true);
            }

            //Color overlays

            for (String color : colorOverlays.keySet()) {

                File colorFolder = new File(tempDir.getAbsolutePath() + File.separator + color);

                for (Overlay overlay : colorOverlays.get(color)) {
                    File file = overlay.build(false);
                    FileUtils.moveFileToDirectory(file, colorFolder, true);
                }


            }


            //Custom style overlays

            for (String name : customStyleOverlays.keySet()) {

                File styleFolder = new File(tempDir.getAbsolutePath() + File.separator + name);

                for (Overlay overlay : customStyleOverlays.get(name)) {
                    File file = overlay.build(false);
                    File targetFolder = new File(styleFolder.getAbsolutePath() + File.separator + overlay.getStyle());

                    FileUtils.moveFileToDirectory(file, targetFolder, true);
                }

            }


            Collection<File> tempFolders = FileUtils.listFilesAndDirs(tempDir, FalseFileFilter.INSTANCE, new OneLevelFileFilter(tempDir));
            tempFolders.remove(tempDir);

            for (File file : tempFolders) {
                ZipUtil.pack(file, new File(file + ".zip"));
                FileUtils.deleteDirectory(file);
            }

            System.out.println("Zip files located in: " + tempDir);
            System.out.println("Opening compiled folder now...");
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + tempDir);

        } else {
            new Overlay(new File(args[1]), new File(args[0])).build(true);
        }

    }
}

