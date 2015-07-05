package com.vaadin.demo.dashboard.view.statistics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.vaadin.maddon.FilterableListContainer;

import com.google.common.eventbus.Subscribe;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.demo.dashboard.domain.Statistics;
import com.vaadin.demo.dashboard.event.DashboardEvent.BrowserResizeEvent;
import com.vaadin.demo.dashboard.event.DashboardEventBus;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings({ "serial" })
public final class StatisticsView extends VerticalLayout implements View {

    private final Table table;
    private static final DateFormat DATEFORMAT = new SimpleDateFormat(
            "MM/dd/yyyy hh:mm:ss a");
    private static final String[] DEFAULT_COLLAPSIBLE = { "time" };

    public StatisticsView() {
        setSizeFull();
        addStyleName("transactions");
        DashboardEventBus.register(this);

        addComponent(buildToolbar());

        table = buildTable();
        addComponent(table);
        setExpandRatio(table, 1);
    }

    @Override
    public void detach() {
        super.detach();
        // A new instance of TransactionsView is created every time it's
        // navigated to so we'll need to clean up references to it on detach.
        DashboardEventBus.unregister(this);
    }

    private Component buildToolbar() {
        HorizontalLayout header = new HorizontalLayout();
        header.addStyleName("viewheader");
        header.setSpacing(true);
        Responsive.makeResponsive(header);

        Label title = new Label("Platform Statistics");
        title.setSizeUndefined();
        title.addStyleName(ValoTheme.LABEL_H1);
        title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
        header.addComponent(title);

        //HorizontalLayout tools = new HorizontalLayout(buildFilter());
        //tools.setSpacing(true);
        //tools.addStyleName("toolbar");
        header.addComponent(buildFilter());

        return header;
    }

    private Component buildFilter() {
    	HorizontalLayout toolbar = new HorizontalLayout();
    	toolbar.addStyleName("toolbar");
        toolbar.setSpacing(true);
    	
    	final DateField  startDate = new DateField();
        startDate.setValue(new Date());
        
        final DateField  endDate = new DateField();
        endDate.setValue(new Date());
        
        Label startLabel = new Label("Start Date");
        toolbar.addComponent(startLabel);
        toolbar.setComponentAlignment(startLabel, Alignment.MIDDLE_CENTER);
        toolbar.addComponent(startDate);
        
        Label endLabel = new Label("End Date");
        toolbar.addComponent(endLabel);
        toolbar.setComponentAlignment(endLabel, Alignment.MIDDLE_CENTER);
        toolbar.addComponent(endDate);
        
        final Button clear = new Button("Filter");
        clear.addStyleName("filterbutton");
        
        toolbar.addComponent(clear);
        
        /*
        filter.addTextChangeListener(new TextChangeListener() {
            @Override
            public void textChange(final TextChangeEvent event) {
                Filterable data = (Filterable) table.getContainerDataSource();
                data.removeAllContainerFilters();
                data.addContainerFilter(new Filter() {
                    @Override
                    public boolean passesFilter(final Object itemId,
                            final Item item) {

                        if (event.getText() == null
                                || event.getText().equals("")) {
                            return true;
                        }

                        return filterByProperty("country", item,
                                event.getText())
                                || filterByProperty("city", item,
                                        event.getText())
                                || filterByProperty("title", item,
                                        event.getText());

                    }

                    @Override
                    public boolean appliesToProperty(final Object propertyId) {
                        if (propertyId.equals("country")
                                || propertyId.equals("city")
                                || propertyId.equals("title")) {
                            return true;
                        }
                        return false;
                    }
                });
            }
        }); */

        //filter.setInputPrompt("Filter");
        //startDate.setIcon(FontAwesome.CALENDAR_O);
        //startDate.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        
        //endDate.setIcon(FontAwesome.CALENDAR_O);
        //endDate.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        
        /*filter.addShortcutListener(new ShortcutListener("Clear",
                KeyCode.ESCAPE, null) {
            @Override
            public void handleAction(final Object sender, final Object target) {
                filter.setValue("");
                ((Filterable) table.getContainerDataSource())
                        .removeAllContainerFilters();
            }
        });*/
        
        return toolbar;
    }

    private Table buildTable() {
        final Table table = new Table() {
            @Override
            protected String formatPropertyValue(final Object rowId,
                    final Object colId, final Property<?> property) {
                String result = super.formatPropertyValue(rowId, colId,
                        property);
                if (colId.equals("time")) {
                    result = DATEFORMAT.format(((Date) property.getValue()));
                }
                
                return result;
            }
        };
        table.setSizeFull();
        table.addStyleName(ValoTheme.TABLE_BORDERLESS);
        table.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TABLE_COMPACT);
        table.setSelectable(true);

        table.setColumnCollapsingAllowed(true);

        table.setColumnReorderingAllowed(true);
        
        List<Statistics> collection = new ArrayList<Statistics>();
        collection.add(new Statistics());
        
        table.setContainerDataSource(new TempStatisticsContainer(collection));
        table.setSortContainerPropertyId("time");
        table.setSortAscending(false);

        table.setVisibleColumns("time", "totalRequests", "successfulRequests", "failedRequests");
        table.setColumnHeaders("Time", "total requests", "successful requests", "failed requests");

        table.setFooterVisible(true);

        // Allow dragging items to the reports menu
        //table.setDragMode(TableDragMode.MULTIROW);
        //table.setMultiSelect(true);

        //table.addActionHandler(new TransactionsActionHandler());
        table.setImmediate(true);

        return table;
    }

    private boolean defaultColumnsVisible() {
        boolean result = true;
        for (String propertyId : DEFAULT_COLLAPSIBLE) {
            if (table.isColumnCollapsed(propertyId) == Page.getCurrent()
                    .getBrowserWindowWidth() < 800) {
                result = false;
            }
        }
        return result;
    }

    @Subscribe
    public void browserResized(final BrowserResizeEvent event) {
        // Some columns are collapsed when browser window width gets small
        // enough to make the table fit better.
        if (defaultColumnsVisible()) {
            for (String propertyId : DEFAULT_COLLAPSIBLE) {
                table.setColumnCollapsed(propertyId, Page.getCurrent()
                        .getBrowserWindowWidth() < 800);
            }
        }
    }

    private boolean filterByProperty(final String prop, final Item item,
            final String text) {
        if (item == null || item.getItemProperty(prop) == null
                || item.getItemProperty(prop).getValue() == null) {
            return false;
        }
        String val = item.getItemProperty(prop).getValue().toString().trim()
                .toLowerCase();
        if (val.contains(text.toLowerCase().trim())) {
            return true;
        }
        return false;
    }

    @Override
    public void enter(final ViewChangeEvent event) {
    }

    private class TempStatisticsContainer extends
            FilterableListContainer<Statistics> {

        public TempStatisticsContainer(
                final Collection<Statistics> collection) {
            super(collection);
        }

        // This is only temporarily overridden until issues with
        // BeanComparator get resolved.
        @Override
        public void sort(final Object[] propertyId, final boolean[] ascending) {
            final boolean sortAscending = ascending[0];
            final Object sortContainerPropertyId = propertyId[0];
            Collections.sort(getBackingList(), new Comparator<Statistics>() {
                @Override
                public int compare(final Statistics o1, final Statistics o2) {
                    int result = 0;
                    /*if ("time".equals(sortContainerPropertyId)) {
                        result = o1.getTime().compareTo(o2.getTime());
                    } else if ("country".equals(sortContainerPropertyId)) {
                        result = o1.getCountry().compareTo(o2.getCountry());
                    } else if ("city".equals(sortContainerPropertyId)) {
                        result = o1.getCity().compareTo(o2.getCity());
                    } else if ("theater".equals(sortContainerPropertyId)) {
                        result = o1.getTheater().compareTo(o2.getTheater());
                    } else if ("room".equals(sortContainerPropertyId)) {
                        result = o1.getRoom().compareTo(o2.getRoom());
                    } else if ("title".equals(sortContainerPropertyId)) {
                        result = o1.getTitle().compareTo(o2.getTitle());
                    } else if ("seats".equals(sortContainerPropertyId)) {
                        result = new Integer(o1.getSeats()).compareTo(o2
                                .getSeats());
                    } else if ("price".equals(sortContainerPropertyId)) {
                        result = new Double(o1.getPrice()).compareTo(o2
                                .getPrice());
                    }

                    if (!sortAscending) {
                        result *= -1;
                    }*/
                    return result;
                }
            });
        }

    }

}
