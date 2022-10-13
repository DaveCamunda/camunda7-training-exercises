package com.camunda.training.payment;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.camunda.training.services.CreditCardService;

@Component
@ExternalTaskSubscription("creditCardCharging")
public class CreditCardHandler implements ExternalTaskHandler {
	
	@Autowired
	public CreditCardService service;
	
	private static final Logger LOG = LoggerFactory.getLogger(CreditCardHandler.class);
	
	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		
		LOG.info("handle topic {} for task id {}", externalTask.getTopicName(), externalTask.getId());
		
	    String cardNumber = externalTask.getVariable("cardNumber"), 
	    		      cvc = externalTask.getVariable("CVC"), 
	    	   expiryDate = externalTask.getVariable("expiryDate");
	    
	    Double openAmount = externalTask.getVariable("openAmount");
	    
	    try {
	    	service.chargeAmount(cardNumber, cvc, expiryDate, openAmount);
	    	
			externalTaskService.complete(externalTask);
	    } catch (IllegalArgumentException e) {
	        StringWriter sw = new StringWriter();
	        
	        e.printStackTrace(new PrintWriter(sw));
	        
	        externalTaskService.handleFailure(externalTask, "credit card expired", sw.toString(), 0, 0);
	    } 

	}
	
}
