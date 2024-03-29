package com.vaadin.demo.dashboard.component;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Credits;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.PlotOptionsPie;
import com.vaadin.demo.dashboard.DashboardUI;
import com.vaadin.demo.dashboard.data.dummy.DummyDataGenerator;
import com.vaadin.demo.dashboard.domain.Movie;

@SuppressWarnings("serial")
public class TopSixTheatersChart extends Chart {

    public TopSixTheatersChart() {
        super(ChartType.PIE);

        setCaption("Service Usage Distribution");
        getConfiguration().setTitle("");
        getConfiguration().getChart().setType(ChartType.PIE);
        getConfiguration().getChart().setAnimation(false);
        setWidth("100%");
        setHeight("90%");

        DataSeries series = new DataSeries();

        //List<Movie> movies = new ArrayList<Movie>(DashboardUI.getDataProvider()
        //        .getMovies());
        
        List<Movie> movies = new ArrayList<Movie>();
        Movie m = new Movie();
        m.setTitle("Friends Finder");
        m.setScore(20);
        movies.add(m);
        
        m = new Movie();
        m.setTitle("My LBS 1");
        m.setScore(10);
        movies.add(m);
        
        m = new Movie();
        m.setTitle("My LBS 2");
        m.setScore(15);
        movies.add(m);
        
        m = new Movie();
        m.setTitle("Awesome LBS");
        m.setScore(9);
        movies.add(m);
        
        m = new Movie();
        m.setTitle("Find Taxi");
        m.setScore(20);
        movies.add(m);
        
        m = new Movie();
        m.setTitle("Find Food");
        m.setScore(5);
        movies.add(m);
        
        for (int i = 0; i < 6; i++) {
            Movie movie = movies.get(i);
            DataSeriesItem item = new DataSeriesItem(movie.getTitle(),
                    movie.getScore());
            series.add(item);
            item.setColor(DummyDataGenerator.chartColors[5 - i]);
        }
        getConfiguration().setSeries(series);

        PlotOptionsPie opts = new PlotOptionsPie();
        opts.setBorderWidth(0);
        opts.setShadow(false);
        opts.setAnimation(false);
        getConfiguration().setPlotOptions(opts);

        Credits c = new Credits("");
        getConfiguration().setCredits(c);
    }

}
