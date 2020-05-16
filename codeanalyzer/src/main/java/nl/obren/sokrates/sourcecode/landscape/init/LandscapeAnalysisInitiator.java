/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.LandscapeGroup;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class LandscapeAnalysisInitiator {
    public LandscapeConfiguration initConfiguration(File analysisRoot, File landscapeConfigFile, boolean saveFile) {
        LandscapeConfiguration landscapeConfiguration = new LandscapeConfiguration();
        landscapeConfiguration.setAnalysisRoot(analysisRoot.getPath());

        LandscapeGroup defaultGroup = new LandscapeGroup();

        landscapeConfiguration.getGroups().add(defaultGroup);

        try (Stream<Path> paths = Files.walk(Paths.get(analysisRoot.getPath()))) {
            paths.filter(file -> isSokratesAnalysisFile(file)).forEach(file -> {
                processAnalysisResultFile(analysisRoot, defaultGroup, file);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }

        if (saveFile) {
            save(landscapeConfigFile, landscapeConfiguration);
        }

        return landscapeConfiguration;
    }

    private void save(File landscapeConfigFile, LandscapeConfiguration landscapeConfiguration) {
        try {
            String json = new JsonGenerator().generate(landscapeConfiguration);
            FileUtils.write(landscapeConfigFile, json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isSokratesAnalysisFile(Path file) {
        return file.endsWith("reports/data/analysisResults.json");
    }

    private void processAnalysisResultFile(File root, LandscapeGroup defaultGroup, Path file) {
        String relativePath = root.toPath().relativize(file).toString();
        defaultGroup.getProjects().add(new SokratesProjectLink(relativePath));

        System.out.println(relativePath);
    }
}
