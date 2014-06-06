package com.acmeair.morphia.services;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.acmeair.entities.Customer;
import com.acmeair.entities.Customer.MemberShipStatus;
import com.acmeair.entities.Customer.PhoneType;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.entities.CustomerSession;
import com.acmeair.morphia.MorphiaConstants;
import com.acmeair.service.DataService;
import com.acmeair.service.CustomerService;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.Morphia;
import com.github.jmkgreen.morphia.query.Query;
import com.mongodb.DB;

//@MorphiaQualifier
@DataService(name=MorphiaConstants.KEY,description=MorphiaConstants.KEY_DESCRIPTION)
public class CustomerServiceImpl implements CustomerService, MorphiaConstants {	
	
	private final static Logger logger = Logger.getLogger(CustomerService.class.getName()); 
	
	//@Resource(name = JNDI_NAME)
	protected DB db;
		
	protected Datastore datastore;
		
	@Inject
	DefaultKeyGeneratorImpl keyGenerator;
	
	
	@PostConstruct
	public void initialization() {		
		Morphia morphia = new Morphia();
		if(db == null){			
	        try {	        	
	        	db = (DB) new InitialContext().lookup(JNDI_NAME);
			} catch (NamingException e) {
				logger.severe("Caught NamingException : " + e.getMessage() );
			}	        
		}
		if(db == null){
			logger.severe("Unable to retreive reference to database, please check the server logs.");
		} else {			
			datastore = morphia.createDatastore(db.getMongo(), db.getName());
		}
	}
	
	@Override
	public Customer createCustomer(String username, String password,
			MemberShipStatus status, int total_miles, int miles_ytd,
			String phoneNumber, PhoneType phoneNumberType,
			CustomerAddress address) {
	

		Customer customer = new Customer(username, password, status, total_miles, miles_ytd, address, phoneNumber, phoneNumberType);
		try{
			datastore.save(customer);
			return customer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Customer updateCustomer(Customer customer) {
		try{
			datastore.save(customer);
			return customer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Customer getCustomer(String username) {
		try{
			
			Query<Customer> q = datastore.find(Customer.class).field("_id").equal(username);
			Customer customer = q.get();					
			return customer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Customer getCustomerByUsername(String username) {
		try{
			Query<Customer> q = datastore.find(Customer.class).field("_id").equal(username);
			Customer customer = q.get();
			if (customer != null) {
				customer.setPassword(null);
			}			
			return customer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean validateCustomer(String username, String password) {
		boolean validatedCustomer = false;
		Customer customerToValidate = getCustomer(username);		
		if (customerToValidate != null) {
			validatedCustomer = password.equals(customerToValidate.getPassword());
		}		
		return validatedCustomer;
	}

	@Override
	public Customer getCustomerByUsernameAndPassword(String username,
			String password) {
		Customer c = getCustomer(username);
		if (!c.getPassword().equals(password)) {
			return null;
		}
		return c;
	}

	@Override
	public CustomerSession validateSession(String sessionid) {
		try {
			Query<CustomerSession> q = datastore.find(CustomerSession.class).field("_id").equal(sessionid);
			
			CustomerSession cSession = q.get();
			if (cSession == null) {
				return null;
			}
			
			Date now = new Date();
			
			if (cSession.getTimeoutTime().before(now)) {
				datastore.delete(cSession);
				return null;
			}
			return cSession;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CustomerSession createSession(String customerId) {
		try {
			String sessionId = keyGenerator.generate().toString();
			Date now = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(now);
			c.add(Calendar.DAY_OF_YEAR, DAYS_TO_ALLOW_SESSION);
			Date expiration = c.getTime();
			CustomerSession cSession = new CustomerSession(sessionId, customerId, now, expiration);
			datastore.save(cSession);
			return cSession;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void invalidateSession(String sessionid) {
		try {
			Query<CustomerSession> q = datastore.find(CustomerSession.class).field("_id").equal(sessionid);
			datastore.delete(q);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
