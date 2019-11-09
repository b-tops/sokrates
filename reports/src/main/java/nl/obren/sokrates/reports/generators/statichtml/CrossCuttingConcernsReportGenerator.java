package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.ScopesRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CrossCuttingConcernsReportGenerator {
    private static final Log LOG = LogFactory.getLog(CrossCuttingConcernsReportGenerator.class);
    private CodeAnalysisResults codeAnalysisResults;
    private int groupCounter = 0;
    private int concernCounter = 0;

    public CrossCuttingConcernsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addCrossCuttingConcernsToReport(RichTextReport report) {
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Cross-cutting concerns are aspects of a software system that cannot be cleanly decomposed from the rest of the system.");
        report.addListItem("A single concern may be present in multiple files. One source code file may contain multiple concerns.");
        report.endUnorderedList();
        report.endSection();

        codeAnalysisResults.getCrossCuttingConcernsAnalysisResults().forEach(crossCuttingConcernsAnalysisResults -> {
            if (crossCuttingConcernsAnalysisResults.getCrossCuttingConcerns().size() > 1) {
                groupCounter++;
                report.startSection("" + groupCounter + " " + crossCuttingConcernsAnalysisResults.getKey().toUpperCase() + " Cross-Cutting Concerns", "");
                report.startUnorderedList();
                int count = crossCuttingConcernsAnalysisResults.getCrossCuttingConcerns().size();
                report.addListItem("The \"" + crossCuttingConcernsAnalysisResults.getKey() + "\" group contains <b>"
                        + count + "</b> concern" + (count > 1 ? "s" : "") + ".");

                report.startUnorderedList();
                crossCuttingConcernsAnalysisResults.getCrossCuttingConcerns().forEach(c -> report.addListItem(c.getName()));
                report.endUnorderedList();

                report.endUnorderedList();
                crossCuttingConcernsAnalysisResults.getCrossCuttingConcerns().forEach(aspectAnalysisResults -> {
                    renderScopes(crossCuttingConcernsAnalysisResults.getKey().toUpperCase(), report, aspectAnalysisResults);
                });
                report.endSection();
            } else {
                report.addParagraph("No cross-cutting concerns defined.");
            }
        });
    }

    private void renderScopes(String key, RichTextReport report, AspectAnalysisResults aspectAnalysisResults) {
        concernCounter++;
        List<NumericMetric> fileCountPerExtension = aspectAnalysisResults.getFileCountPerExtension();
        List<NumericMetric> linesOfCodePerExtension = aspectAnalysisResults.getLinesOfCodePerExtension();

        ScopesRenderer renderer = new ScopesRenderer();
        renderer.setLinesOfCodeInMain(codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode());

        String name = aspectAnalysisResults.getName();
        String title = key + " / " + name.replace(" - ", " Multiple Classifications / ");
        renderer.setTitle(groupCounter + "." + concernCounter + " " + title);
        renderer.setDescription("");
        if (name.equalsIgnoreCase("Unclassified")) {
            renderer.setDescription("This concern include all files that are not included in any of the previously described concerns in this group.");
        }
        if (name.equalsIgnoreCase("Multiple Classifications")) {
            renderer.setDescription("This concern include all files that are included in two or more concerns in this group.");
        }

        renderer.setFileCountPerComponent(fileCountPerExtension);
        renderer.setAspect(aspectAnalysisResults.getAspect());
        renderer.setLinesOfCode(linesOfCodePerExtension);
        renderer.setMaxFileCount(codeAnalysisResults.getMainAspectAnalysisResults().getFilesCount());
        int mainLoc = codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        renderer.setMaxLinesOfCode(mainLoc);
        renderer.setTotalNumberOfRegexMatches(aspectAnalysisResults.getNumberOfRegexLineMatches());

        double relativeSizeInPerc = 100.0 * aspectAnalysisResults.getLinesOfCode() / mainLoc;
        int numberOfFiles = aspectAnalysisResults.getFilesCount();
        int linesOfCode = aspectAnalysisResults.getLinesOfCode();

        report.startSubSection(renderer.getTitle(), "");
        report.addContentInDiv(getCodePercentageSvg(relativeSizeInPerc, title, numberOfFiles, linesOfCode, 200, 20), "");
        renderer.renderDetails(report, false);


        if (name.contains(" - ") && name.contains(" AND ")) {
            List<Double> percentages = extractPercentages(name);
            if (percentages.size() == 2) {
                String svg = getOverlapSvg(percentages);
                report.addListItem(svg);
            }
        }

        codeAnalysisResults.getLogicalDecompositionsAnalysisResults()
                .forEach(logicalDecompositionAnalysisResults -> {
                    report.addHorizontalLine();
                    report.addLevel3Header("per component - " + logicalDecompositionAnalysisResults.getKey() + " logical decomposition");
                    logicalDecompositionAnalysisResults.getComponents()
                            .stream().sorted(Comparator.comparingInt(AspectAnalysisResults::getLinesOfCode).reversed())
                            .forEach(component -> {
                                int componentLoc = component.getLinesOfCode();
                                int[] loc = {0};
                                aspectAnalysisResults.getAspect().getSourceFiles()
                                        .forEach(sourceFile -> {
                                            if (sourceFile.getLogicalComponents().contains(component.getAspect())) {
                                                loc[0] += sourceFile.getLinesOfCode();
                                            }
                                        });
                                double relativeComponentSizeInPerc = 100.0 * loc[0] / componentLoc;
                                String svg = getCodePercentageSvg(relativeComponentSizeInPerc,
                                        component.getName(), component.getFilesCount(),
                                        loc[0],
                                        (int) ((double) 400 * componentLoc / mainLoc), 20);
                                report.startDiv("");
                                report.addHtmlContent(svg);
                                report.endDiv();
                            });
                });

        report.endSection();
    }

    private String getCodePercentageSvg(double percentage, String aspectName, int numberOfFiles, int linesOfCode, int maxSize, int barHeight) {
        String displayText = "in " + numberOfFiles + (numberOfFiles == 1 ? " file " : " files, ")
                + "containing " + FormattingUtils.getFormattedCount(linesOfCode) + " LOC ("
                + FormattingUtils.getFormattedPercentage(percentage) + "%)";

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setMaxBarWidth(maxSize);
        chart.setBarHeight(barHeight);

        return chart.getPercentageSvg(percentage, aspectName, displayText);
    }

    private String getOverlapSvg(List<Double> percentages) {
        Double perc1 = percentages.get(0);
        Double perc2 = percentages.get(1);
        double size1 = perc1 <= perc2 ? 100 : 100 * (perc2 / perc1);
        double size2 = perc1 > perc2 ? 100 : 100 * (perc1 / perc2);
        String svg = "<svg width='220' height='40'>";
        svg += "<rect width='"
                + (int) (size1) + "' x='2' y='2' height='20' style='fill-opacity:0.4;fill:#c0deed;stroke:#898989;stroke-width:2;' />";
        svg += "<rect x='" + (2 + (int) (size1 * (100 - perc1) / 100.0)) + "' y='6' width='"
                + (int) (size2) + "' height='20' style='fill-opacity:0.4;fill:#c0deed;fill:#c0deed;stroke:#898989;stroke-width:2;' />";
        svg += "</svg>";
        return svg;
    }

    private List<Double> extractPercentages(String name) {
        List<Double> percentages = new ArrayList<>();
        try {
            Pattern soe = Pattern.compile("\\(.*?\\%\\)");
            Matcher matcher = soe.matcher(name);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String strValue = name.substring(start + 1, end - 2);
                if (NumberUtils.isNumber(strValue)) {
                    percentages.add(Double.parseDouble(strValue));
                }
            }
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        } catch (StackOverflowError e) {
            LOG.error(e);
        }

        return percentages;
    }
}
