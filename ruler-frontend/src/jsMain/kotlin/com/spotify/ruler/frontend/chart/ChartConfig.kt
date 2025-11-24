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

package com.spotify.ruler.frontend.chart

import com.spotify.ruler.frontend.binding.*

/** Base config for displaying charts. Check https://apexcharts.com/docs/options/ for all chart types and options. */
abstract class ChartConfig {

    /** Returns the chart options for this config used by ApexCharts. */
    abstract fun getOptions(): ApexChartOptions

    /** Utility function which allows concrete configs to start with a common sets of defaults. */
    protected fun buildOptions(builder: ApexChartOptions.() -> Unit): ApexChartOptions {
        val options = js("({})").unsafeCast<ApexChartOptions>()
        
        options.chart = js("({})").unsafeCast<ChartOptions>().apply {
            fontFamily = FONT_FAMILY
            toolbar = js("({})").unsafeCast<ToolbarOptions>().apply {
                show = false
            }
        }
        
        options.dataLabels = js("({})").unsafeCast<DataLabelOptions>().apply {
            enabled = false
        }
        
        options.fill = js("({})").unsafeCast<FillOptions>().apply {
            opacity = 1.0
        }
        
        options.grid = js("({})").unsafeCast<GridOptions>().apply {
            xaxis = js("({})").unsafeCast<GridAxisOptions>().apply {
                lines = js("({})").unsafeCast<GridAxisLineOptions>()
            }
            yaxis = js("({})").unsafeCast<GridAxisOptions>().apply {
                lines = js("({})").unsafeCast<GridAxisLineOptions>()
            }
        }
        
        options.legend = js("({})").unsafeCast<LegendOptions>().apply {
            fontSize = FONT_SIZE
            markers = js("({})").unsafeCast<LegendMarkerOptions>().apply {
                width = FONT_SIZE
                height = FONT_SIZE
            }
        }
        
        options.plotOptions = js("({})").unsafeCast<PlotOptions>().apply {
            bar = js("({})").unsafeCast<BarPlotOptions>()
        }
        
        options.stroke = js("({})").unsafeCast<StrokeOptions>().apply {
            show = true
            colors = arrayOf("transparent")
            width = STROKE_WIDTH
        }
        
        options.tooltip = js("({})").unsafeCast<TooltipOptions>().apply {
            x = js("({})").unsafeCast<TooltipAxisOptions>()
            y = js("({})").unsafeCast<TooltipAxisOptions>()
        }
        
        options.xaxis = js("({})").unsafeCast<AxisOptions>().apply {
            labels = js("({})").unsafeCast<AxisLabelOptions>().apply {
                style = js("({})").unsafeCast<AxisLabelStyleOptions>().apply {
                    fontSize = FONT_SIZE
                }
            }
        }
        
        options.yaxis = js("({})").unsafeCast<AxisOptions>().apply {
            labels = js("({})").unsafeCast<AxisLabelOptions>().apply {
                style = js("({})").unsafeCast<AxisLabelStyleOptions>().apply {
                    fontSize = FONT_SIZE
                }
            }
        }
        
        return options.apply(builder)
    }

    private companion object {
        const val FONT_FAMILY = "var(--bs-body-font-family)"
        const val FONT_SIZE = 14
        const val STROKE_WIDTH = 3
    }
}
