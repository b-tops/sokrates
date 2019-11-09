package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.googlecharts.Palette;
import nl.obren.sokrates.common.renderingutils.googlecharts.PieChart;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PieChartUtils {
    public static String getRiskDistributionPieChart(RiskDistributionStats distribution, List<String> labels) {
        Palette palette = Palette.getRiskPalette();

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setBarHeight(100);
        chart.setMaxBarWidth(400);
        chart.setBarStartXOffset(0);

        int totalValue = distribution.getTotalValue();

        List<Integer> values = RiskDistributionStatsReportUtils.getRowData(distribution);

        String joinedValues = values.stream().map(v -> FormattingUtils.getFormattedPercentage(100.0 * v.doubleValue() / totalValue) + "%").collect(Collectors.joining(" | "));
        String stackedBarSvg = chart.getStackedBarSvg(values, palette, distribution.getKey(), joinedValues);

        String html = "";
        html += "<div>" + stackedBarSvg + "</div>";
        html += "<div style='font-size:90%;margin-top:20px;width:100%;text-alight:right'>Legend: " + chart.getLegend(labels, palette) + "</div>";

        return html;
    }
}
