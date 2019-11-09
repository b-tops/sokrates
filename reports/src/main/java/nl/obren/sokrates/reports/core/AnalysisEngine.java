package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.analysis.Finding;
import nl.obren.sokrates.common.analysis.ValidationMessage;

import java.util.List;

public interface AnalysisEngine<T extends Scope> {
    List<ValidationMessage> validate(T scope);
    List<Finding> analyze(T scope);
}
