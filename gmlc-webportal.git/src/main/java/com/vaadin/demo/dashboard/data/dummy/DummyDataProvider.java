package com.vaadin.demo.dashboard.data.dummy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaadin.data.Property;
import com.vaadin.data.util.filter.Between;
import com.vaadin.data.util.filter.Compare.Equal;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.connection.J2EEConnectionPool;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import com.vaadin.data.util.sqlcontainer.query.OrderBy;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.demo.dashboard.data.DataProvider;
import com.vaadin.demo.dashboard.domain.DashboardNotification;
import com.vaadin.demo.dashboard.domain.Movie;
import com.vaadin.demo.dashboard.domain.MovieRevenue;
import com.vaadin.demo.dashboard.domain.Transaction;
import com.vaadin.demo.dashboard.domain.Statistics;
import com.vaadin.demo.dashboard.domain.Detailed;
import com.vaadin.demo.dashboard.domain.Service;
import com.vaadin.demo.dashboard.domain.Location;
import com.vaadin.demo.dashboard.domain.User;
import com.vaadin.server.VaadinRequest;
import com.vaadin.util.CurrentInstance;

/**
 * A dummy implementation for the backend API.
 */
public class DummyDataProvider implements DataProvider {

    // TODO: Get API key from http://developer.rottentomatoes.com
    private static final String ROTTEN_TOMATOES_API_KEY = null;

    /* List of countries and cities for them */
    private static Multimap<String, String> countryToCities;
    private static Date lastDataUpdate;
    private static Collection<Movie> movies;
    private static Multimap<Long, Transaction> transactions;
    private static Multimap<Long, MovieRevenue> revenue;

    private static Random rand = new Random();

    private final Collection<DashboardNotification> notifications = DummyDataGenerator
            .randomNotifications();

    private JDBCConnectionPool connectionPool;
    
    /**
     * Initialize the data for this application.
     * @throws SQLException 
     */
    public DummyDataProvider() {
    	connectionPool = new J2EEConnectionPool("java:comp/env/jdbc/tldb");
    	
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        if (lastDataUpdate == null || lastDataUpdate.before(cal.getTime())) {
            refreshStaticData();
            lastDataUpdate = new Date();
        }
    }

    private void refreshStaticData() {
        countryToCities = loadTheaterData();
        movies = loadMoviesData();
    }

    /**
     * Get a list of movies currently playing in theaters.
     *
     * @return a list of Movie objects
     */
    @Override
    public Collection<Movie> getMovies() {
        return Collections.unmodifiableCollection(movies);
    }

    /**
     * Initialize the list of movies playing in theaters currently. Uses the
     * Rotten Tomatoes API to get the list. The result is cached to a local file
     * for 24h (daily limit of API calls is 10,000).
     *
     * @return
     */
    private static Collection<Movie> loadMoviesData() {

        JsonObject json = null;
        File cache;
        VaadinRequest vaadinRequest = CurrentInstance.get(VaadinRequest.class);

        File baseDirectory = vaadinRequest.getService().getBaseDirectory();
        cache = new File(baseDirectory + "/movies.txt");

        try {
            if (cache.exists()
                    && System.currentTimeMillis() < cache.lastModified()
                            + (1000 * 60 * 60 * 24)) {
                // Use cache if it's under 24h old
                json = readJsonFromFile(cache);
            } else {
                if (ROTTEN_TOMATOES_API_KEY != null) {
                    try {
                        json = readJsonFromUrl("http://api.rottentomatoes.com/api/public/v1.0/lists/movies/in_theaters.json?page_limit=30&apikey="
                                + ROTTEN_TOMATOES_API_KEY);
                        // Store in cache
                        FileWriter fileWriter = new FileWriter(cache);
                        fileWriter.write(json.toString());
                        fileWriter.close();
                    } catch (Exception e) {
                        json = readJsonFromFile(new File(baseDirectory
                                + "/movies-fallback.txt"));
                    }
                } else {
                    json = readJsonFromFile(new File(baseDirectory
                            + "/movies-fallback.txt"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collection<Movie> result = new ArrayList<Movie>();
        if (json != null) {
            JsonArray moviesJson;

            moviesJson = json.getAsJsonArray("movies");
            for (int i = 0; i < moviesJson.size(); i++) {
                JsonObject movieJson = moviesJson.get(i).getAsJsonObject();
                JsonObject posters = movieJson.get("posters").getAsJsonObject();
                if (!posters.get("profile").getAsString()
                        .contains("poster_default")) {
                    Movie movie = new Movie();
                    movie.setId(i);
                    movie.setTitle(movieJson.get("title").getAsString());
                    try {
                        movie.setDuration(movieJson.get("runtime").getAsInt());
                    } catch (Exception e) {
                        // No need to handle this exception
                    }
                    movie.setSynopsis(movieJson.get("synopsis").getAsString());
                    movie.setThumbUrl(posters.get("profile").getAsString()
                            .replace("_tmb", "_320"));
                    movie.setPosterUrl(posters.get("detailed").getAsString()
                            .replace("_tmb", "_640"));

                    try {
                        JsonObject releaseDates = movieJson
                                .get("release_dates").getAsJsonObject();
                        String datestr = releaseDates.get("theater")
                                .getAsString();
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        movie.setReleaseDate(df.parse(datestr));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        movie.setScore(movieJson.get("ratings")
                                .getAsJsonObject().get("critics_score")
                                .getAsInt());
                    } catch (Exception e) {
                        // No need to handle this exception
                    }

                    result.add(movie);

                }
            }
        }
        return result;
    }

    /* JSON utility method */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /* JSON utility method */
    private static JsonObject readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is,
                    Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JsonElement jelement = new JsonParser().parse(jsonText);
            JsonObject jobject = jelement.getAsJsonObject();
            return jobject;
        } finally {
            is.close();
        }
    }

    /* JSON utility method */
    private static JsonObject readJsonFromFile(File path) throws IOException {
        BufferedReader rd = new BufferedReader(new FileReader(path));
        String jsonText = readAll(rd);
        JsonElement jelement = new JsonParser().parse(jsonText);
        JsonObject jobject = jelement.getAsJsonObject();
        return jobject;
    }

    /**
     * =========================================================================
     * Countries, cities, theaters and rooms
     * =========================================================================
     */

    static List<String> theaters = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;
        {
            add("Threater 1");
            add("Threater 2");
            add("Threater 3");
            add("Threater 4");
            add("Threater 5");
            add("Threater 6");
        }
    };

    static List<String> rooms = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;
        {
            add("Room 1");
            add("Room 2");
            add("Room 3");
            add("Room 4");
            add("Room 5");
            add("Room 6");
        }
    };

    /**
     * Parse the list of countries and cities
     */
    private static Multimap<String, String> loadTheaterData() {

        /* First, read the text file into a string */
        StringBuffer fileData = new StringBuffer(2000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                DummyDataProvider.class.getResourceAsStream("cities.txt")));

        char[] buf = new char[1024];
        int numRead = 0;
        try {
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String list = fileData.toString();

        /*
         * The list has rows with tab delimited values. We want the second (city
         * name) and last (country name) values, and build a Map from that.
         */
        Multimap<String, String> countryToCities = MultimapBuilder.hashKeys()
                .arrayListValues().build();
        for (String line : list.split("\n")) {
            String[] tabs = line.split("\t");
            String city = tabs[1];
            String country = tabs[tabs.length - 2];

            if (!countryToCities.containsKey(country)) {
                countryToCities.putAll(country, new ArrayList<String>());
            }
            countryToCities.get(country).add(city);
        }

        return countryToCities;

    }

    public static Movie getMovieForTitle(String title) {
        for (Movie movie : movies) {
            if (movie.getTitle().equals(title)) {
                return movie;
            }
        }
        return null;
    }

    @Override
    public User authenticate(String userName, String password) {
        User user = new User();
        user.setFirstName(DummyDataGenerator.randomFirstName());
        user.setLastName(DummyDataGenerator.randomLastName());
        user.setRole("admin");
        String email = user.getFirstName().toLowerCase() + "."
                + user.getLastName().toLowerCase() + "@"
                + DummyDataGenerator.randomCompanyName().toLowerCase() + ".com";
        user.setEmail(email.replaceAll(" ", ""));
        user.setLocation(DummyDataGenerator.randomWord(5, true));
        user.setBio("Quis aute iure reprehenderit in voluptate velit esse."
                + "Cras mattis iudicium purus sit amet fermentum.");
        return user;
    }

    @Override
    public Collection<MovieRevenue> getDailyRevenuesByMovie(long id) {
        return Collections.unmodifiableCollection(revenue.get(id));
    }

    private Date getDay(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTime();
    }

    @Override
    public Collection<MovieRevenue> getTotalMovieRevenues() {
        return Collections2.transform(movies,
                new Function<Movie, MovieRevenue>() {
                    @Override
                    public MovieRevenue apply(Movie input) {
                        return Iterables.getLast(getDailyRevenuesByMovie(input
                                .getId()));
                    }
                });
    }

    @Override
    public int getUnreadNotificationsCount() {
        Predicate<DashboardNotification> unreadPredicate = new Predicate<DashboardNotification>() {
            @Override
            public boolean apply(DashboardNotification input) {
                return !input.isRead();
            }
        };
        return Collections2.filter(notifications, unreadPredicate).size();
    }

    @Override
    public Collection<DashboardNotification> getNotifications() {
        for (DashboardNotification notification : notifications) {
            notification.setRead(true);
        }
        return Collections.unmodifiableCollection(notifications);
    }

    @Override
    public double getTotalSum() {
        double result = 0;
        return result;
    }

    @Override
    public Movie getMovie(final long movieId) {
        return Iterables.find(movies, new Predicate<Movie>() {
            @Override
            public boolean apply(Movie input) {
                return input.getId() == movieId;
            }
        });
    }

    @Override
    public Collection<Transaction> getTransactionsBetween(final Date startDate,
            final Date endDate) {
    	return null;
    }

	@Override
	public Collection<Transaction> getTransactionBetween(Date startDate,
			Date endDate) {
		
		FreeformQuery query = new FreeformQuery("SELECT * FROM SAMPLE", this.connectionPool, "ID");
		//SQLContainer container = new SQLContainer(query);
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Transaction> getTransactions(String refId, Date startDate, Date endDate) {
		TableQuery query = new TableQuery("transaction_tab", this.connectionPool);
		
		//FreeformQuery query = new FreeformQuery(
		//		String.format("select * from transaction_tab where ref_id = '%s'", refId), this.connectionPool, "id");
		
		SQLContainer container;
		List<Transaction> list = new ArrayList<Transaction>();
		
		try {
			
			container = new SQLContainer(query);
			if(refId != null && refId.trim().length() > 0)
				container.addContainerFilter(new Equal("ref_id", refId));
			
			if(startDate != null && endDate != null) {
				Calendar calEndDate = Calendar.getInstance();
				calEndDate.setTime(endDate);
				calEndDate.add(Calendar.DAY_OF_MONTH, 1);
				container.addContainerFilter(new Between("date_time", startDate, calEndDate.getTime()));
			}
			
			container.addOrderBy(new OrderBy("id", false));
			container.addOrderBy(new OrderBy("seq_id", false));
			
			Transaction transaction = null;
			
			Collection<?> itemIDS = container.getItemIds();
			for (Object itemID : itemIDS) {
				transaction = new Transaction();
				
				Property<String> pRefId = container.getContainerProperty(itemID, "ref_id");
				transaction.setRefId(pRefId.getValue());
				
				Property<String> pModule = container.getContainerProperty(itemID, "module_name");
				transaction.setModuleName(pModule.getValue());
				
				Property<Date> pDate = container.getContainerProperty(itemID, "date_time");
				transaction.setDateTime(pDate.getValue());
				
				Property<Integer> pSeqId = container.getContainerProperty(itemID, "seq_id");
				transaction.setSequenceId(pSeqId.getValue());
				
				Property<Integer> pId = container.getContainerProperty(itemID, "id");
				transaction.setId(pId.getValue());
				
				list.add(transaction);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}

	@Override
	public Collection<Transaction> getRecentTransactions(int count) {
		return null;
	}
	
	//Statictics
	 @Override
	    public Collection<Statistics> getStatisticsBetween(final Date startDate,
	            final Date endDate) {
	    	return null;
	    }

		@Override
		public Collection<Statistics> getStatisticBetween(Date startDate,
				Date endDate) {
			
			FreeformQuery query = new FreeformQuery("SELECT * FROM SAMPLE", this.connectionPool, "ID");
			//SQLContainer container = new SQLContainer(query);
			
			return null;
		}
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Statistics> getStatistics(Date startDate, Date endDate) {
//		TableQuery query = new TableQuery("transaction_tab", this.connectionPool);
		
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(endDate); // Now use today date.
		c.add(Calendar.DATE, 1); // Adding 1 day
		
		
		FreeformQuery query = new FreeformQuery("SELECT date_time, COUNT(*) AllRequests, SUM(case when cell_id IS NULL OR cell_id = '' then 1 else 0 end) Failed, SUM(case when cell_id IS NOT NULL AND cell_id <> '' then 1 else 0 end) Success FROM transaction_tab  WHERE (date_time BETWEEN  '"+ dt.format(startDate)+"' AND '"+dt.format(c.getTime())+"') group by DATE(date_time)", this.connectionPool, "date_time");
		
				
		SQLContainer container;
		List<Statistics> list = new ArrayList<Statistics>();
		
		try {
			
			container = new SQLContainer(query);
			
			if(startDate != null && endDate != null) {
				Calendar calEndDate = Calendar.getInstance();
				calEndDate.setTime(endDate);
				calEndDate.add(Calendar.DAY_OF_MONTH, 1);
				container.addContainerFilter(new Between("date_time", startDate, calEndDate.getTime()));
			}
			
			
			//container.addOrderBy(new OrderBy("date_time", false));
			//container.addOrderBy(new OrderBy("seq_id", false));
			
			Statistics statistics = null;
			
			Collection<?> itemIDS = container.getItemIds();
			for (Object itemID : itemIDS) {
				statistics = new Statistics();
				
				Property<Date> pDate = container.getContainerProperty(itemID, "date_time");
				statistics.setTime(pDate.getValue());
				
				Property<Long> pTotalReq = container.getContainerProperty(itemID, "AllRequests");
				statistics.setTotalRequests(pTotalReq.getValue());
				
				Property<BigDecimal> pSuccessReq = container.getContainerProperty(itemID, "Success");				
				statistics.setSuccessfulRequests(pSuccessReq.getValue());
											
				Property<BigDecimal> pFailedReq = container.getContainerProperty(itemID, "Failed");
				statistics.setFailedRequest(pFailedReq.getValue());
				
				
				list.add(statistics);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	//Statictics
	
	 @Override
	    public Collection<Detailed> getDetailedBetween(final Date startDate, final Date endDate) {
	    	return null;
	    }
	
		@Override
		public Collection<Detailed> getDetailedsBetween(Date startDate,Date endDate) {
			
			FreeformQuery query = new FreeformQuery("SELECT * FROM SAMPLE", this.connectionPool, "ID");
			//SQLContainer container = new SQLContainer(query);
				
				return null;
			}
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Detailed> getDetailed(Date startDate, Date endDate) {
//		TableQuery query = new TableQuery("transaction_tab", this.connectionPool);
		
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(endDate); // Now use today date.
		c.add(Calendar.DATE, 1); // Adding 1 day
		
		
		FreeformQuery query = new FreeformQuery("SELECT date_time, service_id, COUNT(*) AllRequests, SUM(case when cell_id IS NULL OR cell_id = '' then 1 else 0 end) Failed, SUM(case when cell_id IS NOT NULL AND cell_id <> '' then 1 else 0 end) Success FROM transaction_tab  WHERE (date_time BETWEEN  '"+ dt.format(startDate)+"' AND '"+dt.format(c.getTime())+"') group by DATE(date_time)", this.connectionPool, "date_time");
		
		SQLContainer container;
		List<Detailed> list = new ArrayList<Detailed>();
		
		try {
			
			container = new SQLContainer(query);
			
			if(startDate != null && endDate != null) {
				Calendar calEndDate = Calendar.getInstance();
				calEndDate.setTime(endDate);
				calEndDate.add(Calendar.DAY_OF_MONTH, 1);
				container.addContainerFilter(new Between("date_time", startDate, calEndDate.getTime()));
			}
			
			
			//container.addOrderBy(new OrderBy("date_time", false));
			//container.addOrderBy(new OrderBy("seq_id", false));
			
			Detailed detailed = null;
			
			Collection<?> itemIDS = container.getItemIds();
			for (Object itemID : itemIDS) {
				detailed = new Detailed();
				
				Property<Date> pDate = container.getContainerProperty(itemID, "date_time");
				detailed.setTime(pDate.getValue());
				
				Property<String> pServerID = container.getContainerProperty(itemID, "service_id");
				detailed.setServiceID(pServerID.getValue());
				
				Property<Long> pTotalReq = container.getContainerProperty(itemID, "AllRequests");
				detailed.setTotalRequests(pTotalReq.getValue());
				
				Property<BigDecimal> pSuccessReq = container.getContainerProperty(itemID, "Success");				
				detailed.setSuccessfulRequests(pSuccessReq.getValue());
											
				Property<BigDecimal> pFailedReq = container.getContainerProperty(itemID, "Failed");
				detailed.setFailedRequest(pFailedReq.getValue());
				
				
				list.add(detailed);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Service> getService(String serviceID, Date startDate, Date endDate) {
//		TableQuery query = new TableQuery("transaction_tab", this.connectionPool);
		
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(endDate); // Now use today date.
		c.add(Calendar.DATE, 1); // Adding 1 day
		
		FreeformQuery query = new FreeformQuery("SELECT seq_id,date_time,msisdn,cell_id from transaction_tab  WHERE service_id='"+serviceID+"' OR (date_time BETWEEN  '"+ dt.format(startDate)+"' AND '"+dt.format(c.getTime())+"')", this.connectionPool, "date_time");		
		
		SQLContainer container;
		List<Service> list = new ArrayList<Service>();
		
		try {
			
			container = new SQLContainer(query);
			if(serviceID != null && serviceID.trim().length() > 0)
				container.addContainerFilter(new Equal("service_id", serviceID));
			
			if(startDate != null && endDate != null) {
				Calendar calEndDate = Calendar.getInstance();
				calEndDate.setTime(endDate);
				calEndDate.add(Calendar.DAY_OF_MONTH, 1);
				container.addContainerFilter(new Between("date_time", startDate, calEndDate.getTime()));
			}
			
			
			//container.addOrderBy(new OrderBy("date_time", false));
			//container.addOrderBy(new OrderBy("seq_id", false));
			
			Service service = null;
			
			Collection<?> itemIDS = container.getItemIds();
			for (Object itemID : itemIDS) {
				service = new Service();
				
				Property<Integer> pId = container.getContainerProperty(itemID, "seq_id");
				service.setId(pId.getValue());
				
				Property<Date> pDate = container.getContainerProperty(itemID, "date_time");
				service.setDateTime(pDate.getValue());
				
				Property<String> msisdn = container.getContainerProperty(itemID, "msisdn");
				service.setMsisdn(msisdn.getValue());
				
				Property<String> pcell_id = container.getContainerProperty(itemID, "cell_id");
				service.setCellId(pcell_id.getValue());
				
				
				list.add(service);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Location> getLocation(String msisdn, Date startDate, Date endDate) {
//		TableQuery query = new TableQuery("transaction_tab", this.connectionPool);
		
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(endDate); // Now use today date.
		c.add(Calendar.DATE, 1); // Adding 1 day
		
		FreeformQuery query = new FreeformQuery("SELECT seq_id,msisdn,service_id,date_time,cell_id from transaction_tab WHERE msisdn='"+msisdn+"' OR (date_time BETWEEN  '"+ dt.format(startDate)+"' AND '"+dt.format(c.getTime())+"')", this.connectionPool, "date_time");		
		
		SQLContainer container;
		List<Location> list = new ArrayList<Location>();
		
		try {
			
			container = new SQLContainer(query);
			if(msisdn != null && msisdn.trim().length() > 0)
				container.addContainerFilter(new Equal("msisdn", msisdn));
			
			if(startDate != null && endDate != null) {
				Calendar calEndDate = Calendar.getInstance();
				calEndDate.setTime(endDate);
				calEndDate.add(Calendar.DAY_OF_MONTH, 1);
				container.addContainerFilter(new Between("date_time", startDate, calEndDate.getTime()));
			}
			
			
			//container.addOrderBy(new OrderBy("date_time", false));
			//container.addOrderBy(new OrderBy("seq_id", false));
			
			Location location = null;
			
			Collection<?> itemIDS = container.getItemIds();
			for (Object itemID : itemIDS) {
				location = new Location();
				

				Property<Integer> pId = container.getContainerProperty(itemID, "seq_id");
				location.setId(pId.getValue());
				
				Property<String> pmsisdn = container.getContainerProperty(itemID, "msisdn");
				location.setMsisdn(pmsisdn.getValue());
				
				Property<String> pserviceId = container.getContainerProperty(itemID, "service_id");
				location.setServiceId(pserviceId.getValue());
				
				Property<Date> pDate = container.getContainerProperty(itemID, "date_time");
				location.setDateTime(pDate.getValue());
				
								
				Property<String> pcell_id = container.getContainerProperty(itemID, "cell_id");
				location.setCellId(pcell_id.getValue());
				
				
				list.add(location);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}

}
