/*
 * Copyright 2021 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotify.ruler.frontend.binding

import org.w3c.dom.Element

typealias NumberFormatter = (Number) -> String
typealias TooltipAxisFormatter = (Number, TooltipAxisFormatterOptions) -> String

// External require function for importing modules
external fun require(module: String): dynamic

// Helper function to create ApexCharts instance
// ApexCharts 5.x uses ES modules - need to handle various export formats
private fun instantiateApexCharts(module: dynamic, element: Element?, options: dynamic): dynamic {
    return js("""
        (function(mod, el, opts) {
            // Handle different module formats
            // ES module default export: mod.default
            // CommonJS: mod itself
            // Named export: mod.ApexCharts
            var ApexChartsConstructor = mod.default || mod.ApexCharts || mod;
            
            if (typeof ApexChartsConstructor !== 'function') {
                console.error('ApexCharts constructor not found!', mod);
                throw new Error('ApexCharts constructor not found');
            }
            
            return new ApexChartsConstructor(el, opts);
        })(module, element, options)
    """)
}

// Wrapper class to properly instantiate ApexCharts from CommonJS/ES module
class ApexCharts(element: Element?, options: dynamic) {
    private val chart: dynamic
    
    init {
        val apexModule = require("apexcharts")
        chart = instantiateApexCharts(apexModule, element, options)
    }
    
    fun render(): dynamic = chart.render()
    fun destroy(): dynamic = chart.destroy()
    fun updateOptions(options: dynamic, redrawPaths: Boolean = true, animate: Boolean = true): dynamic {
        return chart.updateOptions(options, redrawPaths, animate)
    }
    fun updateSeries(newSeries: Array<Series>, animate: Boolean = true): dynamic {
        return chart.updateSeries(newSeries, animate)
    }
}

external interface ApexChartOptions {
    var chart: ChartOptions
    var dataLabels: DataLabelOptions
    var fill: FillOptions
    var grid: GridOptions
    var legend: LegendOptions
    var plotOptions: PlotOptions
    var series: Array<Series>
    var stroke: StrokeOptions
    var tooltip: TooltipOptions
    var xaxis: AxisOptions
    var yaxis: AxisOptions
}

external interface AxisLabelOptions {
    var style: AxisLabelStyleOptions
    var formatter: NumberFormatter
}

external interface AxisLabelStyleOptions {
    var fontSize: Int
}

external interface AxisOptions {
    var categories: Array<String>
    var labels: AxisLabelOptions
}

external interface BarPlotOptions {
    var horizontal: Boolean
}

external interface ChartOptions {
    var fontFamily: String
    var height: Int
    var toolbar: ToolbarOptions
    var type: String
}

external interface DataLabelOptions {
    var enabled: Boolean
}

external interface FillOptions {
    var opacity: Double
}

external interface GridAxisLineOptions {
    var show: Boolean
}

external interface GridAxisOptions {
    var lines: GridAxisLineOptions
}

external interface GridOptions {
    var xaxis: GridAxisOptions
    var yaxis: GridAxisOptions
}

external interface LegendMarkerOptions {
    var width: Int
    var height: Int
}

external interface LegendOptions {
    var fontSize: Int
    var markers: LegendMarkerOptions
}

external interface PlotOptions {
    var bar: BarPlotOptions
}

external interface Series {
    var name: String
    var data: Array<Number>
}

external interface StrokeOptions {
    var show: Boolean
    var colors: Array<String>
    var width: Int
}

external interface ToolbarOptions {
    var show: Boolean
}

external interface TooltipAxisFormatterOptions {
    var series: Array<Array<Number>>
    var seriesIndex: Int
}

external interface TooltipAxisOptions {
    var formatter: TooltipAxisFormatter
}

external interface TooltipOptions {
    var x: TooltipAxisOptions
    var y: TooltipAxisOptions
}
