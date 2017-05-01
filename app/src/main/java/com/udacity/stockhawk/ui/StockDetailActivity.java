package com.udacity.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.DashPathEffect;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StockDetailActivity extends AppCompatActivity  implements LoaderManager.LoaderCallbacks<Cursor> {

    final Locale mLocale = new Locale("en", "US");
    final NumberFormat mCurrencyFormatter = NumberFormat.getCurrencyInstance(mLocale);
    final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyy/MM");

    public class ChartMarkerView extends MarkerView {

        private TextView tvContent;

        public ChartMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);

            tvContent = (TextView) findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {

            if (e instanceof CandleEntry) {

                CandleEntry ce = (CandleEntry) e;

                tvContent.setText("" + Utils.formatNumber(ce.getHigh(), 0, true));
            } else {

                tvContent.setText(String.format(Locale.US, "%s\n%s",
                        mDateFormatter.format(new Date((long)e.getX())),
                        mCurrencyFormatter.format(e.getY())));
            }

            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2), -getHeight());
        }
    }

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.stock_chart)
    LineChart mChart;

    private String mSymbol;

    private static final int STOCK_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        ButterKnife.bind(this);

        //enable back button to return to parent activity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //read symbol from extras
        mSymbol = getIntent().getStringExtra(Contract.Quote.COLUMN_SYMBOL);

        getSupportActionBar().setTitle(String.format(Locale.US, "%s %s", mSymbol, getString(R.string.history)));

        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                Contract.Quote.COLUMN_SYMBOL + " = \"" + mSymbol + "\"", null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            String historyString = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
            String[] historyArray = historyString.split("\n");
            if (historyArray.length > 0) {
                ArrayList<Entry> values = new ArrayList<Entry>();

                for (String history : historyArray) {
                    String[] valuePair = history.split(", ");
                    String timeString = valuePair[0];
                    String valueString = valuePair[1];
                    long time = Long.parseLong(timeString);
                    float value = Float.parseFloat(valueString);
                    values.add(0, new Entry(time, value));
                }

                configureChart(values);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId()== android.R.id.home) {
            //Finish the details activity (this) to return to the main activity without destroying it's bundle data
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void configureChart(ArrayList<Entry> values) {

        mChart.setDrawGridBackground(false);

        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(false);

        mChart.setExtraRightOffset(30);
        mChart.setPinchZoom(false);

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        ChartMarkerView mv = new ChartMarkerView(this, R.layout.chart_marker);
        mv.setChartView(mChart); // For bounds control
        mChart.setMarker(mv); // Set the marker to the chart

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setLabelCount(3, true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mDateFormatter.format(new Date((long)value));
            }
        });

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
        leftAxis.setAxisMinimum(0f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));

        leftAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mCurrencyFormatter.format(value);
            }
        });

        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);

        mChart.getAxisRight().setEnabled(false);

        // get the legend (only possible after setting data)
        Legend legend = mChart.getLegend();

        legend.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));

        // modify the legend ...
        legend.setForm(Legend.LegendForm.LINE);

        LineDataSet dataSet;

        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {
            dataSet = (LineDataSet)mChart.getData().getDataSetByIndex(0);
            dataSet.setValues(values);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            dataSet = new LineDataSet(values, mSymbol);

            dataSet.setColor(Color.WHITE);
            dataSet.setDrawCircles(false);
            dataSet.setLineWidth(2f);
            dataSet.setValueTextSize(12f);
            dataSet.setDrawFilled(false);
            dataSet.setFormLineWidth(1f);
            dataSet.setFormSize(15.f);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(dataSet); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(dataSets);

            // set data
            mChart.setData(data);
        }

        mChart.invalidate();

    }
}
