package com.acmeair.morphia.services;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.mongodb.morphia.Datastore;

import com.acmeair.entities.Booking;
import com.acmeair.entities.BookingPK;
import com.acmeair.entities.Customer;
import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightPK;
import com.acmeair.morphia.MorphiaConstants;
import com.acmeair.morphia.services.util.MongoConnectionManager;
import com.acmeair.service.BookingService;
import com.acmeair.service.CustomerService;
import com.acmeair.service.DataService;
import com.acmeair.service.FlightService;
import com.acmeair.service.ServiceLocator;
import org.mongodb.morphia.*;
import org.mongodb.morphia.query.Query;
import com.mongodb.DB;


@DataService(name=MorphiaConstants.KEY,description=MorphiaConstants.KEY_DESCRIPTION)
public class BookingServiceImpl implements BookingService, MorphiaConstants {

	private final static Logger logger = Logger.getLogger(BookingService.class.getName()); 

		
	Datastore datastore;
	
	@Inject 
	DefaultKeyGeneratorImpl keyGenerator;
	
	private FlightService flightService = ServiceLocator.instance().getService(FlightService.class);
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);


	@PostConstruct
	public void initialization() {	
		datastore = MongoConnectionManager.getConnectionManager().getDatastore();	
	}	
	
	
	@Override
	public BookingPK bookFlight(String customerId, FlightPK flightId) {
		try{
			Flight f = flightService.getFlightByFlightKey(flightId);
			Customer c = customerService.getCustomerByUsername(customerId);
			
			Booking newBooking = new Booking(keyGenerator.generate().toString(), new Date(), c, f);

			datastore.save(newBooking);
			return newBooking.getPkey();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Booking getBooking(String user, String id) {
		try{
			Query<Booking> q = datastore.find(Booking.class).field("_id").equal(new BookingPK(user, id));
			Booking booking = q.get();
			
			return booking;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Booking> getBookingsByUser(String user) {
		try{
			Query<Booking> q = datastore.find(Booking.class).field("pkey.customerId").equal(user);
			List<Booking> bookings = q.asList();
			
			return bookings;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void cancelBooking(String user, String id) {
		try{
			datastore.delete(Booking.class, new BookingPK(user, id) );
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public Long count() {
		return datastore.find(Booking.class).countAll();
	}	
}
