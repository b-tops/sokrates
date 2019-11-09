package nl.obren.sokrates.sourcecode.lang.css;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class CssAnalyzer extends LanguageAnalyzer {
    public CssAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        String content = emptyComments(sourceFile);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = emptyComments(sourceFile);

        content = SourceCodeCleanerUtils.trimLines(content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    private String emptyComments(SourceFile sourceFile) {
        String content = sourceFile.getContent();

        content = SourceCodeCleanerUtils.emptyComments(content, null, "/*", "*/").getCleanedContent();

        return content;
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new ArrayList<>();
    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new DependenciesAnalysis();
    }

}
