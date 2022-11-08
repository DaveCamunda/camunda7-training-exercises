package com.camunda.training;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
@Deployment(resources = { "PaymentProcess.bpmn" })
public class PaymentUnitTest {
  
	@Test
    public void testHappyPath() {
		
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("PaymentProcess", withVariables("orderTotal", 45.99, "customerCredit", 30.00));
	    
		assertThat(processInstance).isWaitingAt("DeductCreditTask").externalTask().hasTopicName("creditDeduction");    
		
		complete(externalTask());
		
	    assertThat(processInstance).isWaitingAt("ChargeCreditCardTask").externalTask().hasTopicName("creditCardCharging");
	    
	    complete(externalTask());
	    
	    assertThat(processInstance).isWaitingAt("PaymentCompletedEvent").externalTask().hasTopicName("paymentCompletion");
	    
	    complete(externalTask());
	    
	    assertThat(processInstance).isEnded().hasPassed("PaymentCompletedEvent");
	    
    }
	
	@Test
	public void testCreditCardFailure() {
		
		ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcess").startBeforeActivity("ChargeCreditCardTask").execute();	
		
		assertThat(processInstance).isWaitingAt("ChargeCreditCardTask");
		
		fetchAndLock("creditCardCharging", "junit-test-worker", 1);
		
	    externalTaskService().handleBpmnError(externalTask().getId(), "junit-test-worker", "creditCardChargeError");
	    
		assertThat(processInstance).isWaitingAt("CheckPaymentUserTask");
		
		complete(task(), withVariables("errorResolved", true));
		
		assertThat(processInstance).isWaitingAt("ChargeCreditCardTask");
		
		complete(externalTask());
	    
	    assertThat(processInstance).isWaitingAt("PaymentCompletedEvent").externalTask().hasTopicName("paymentCompletion");
	    
	    complete(externalTask());
	    
	    assertThat(processInstance).isEnded().hasPassed("PaymentCompletedEvent");
	    
	}
	
	@Test
	public void customerCreditCompensationTest() {
		
		ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcess").startBeforeActivity("DeductCreditTask").setVariables(withVariables("orderTotal", 50)).execute();
		    
		assertThat(processInstance).isWaitingAt("DeductCreditTask");
		
		complete(externalTask(), withVariables("customerCredit", 30));
		
		fetchAndLock("creditCardCharging", "junit-test-worker", 1);
		
		externalTaskService().handleBpmnError(externalTask().getId(), "junit-test-worker", "creditCardChargeError");
		
		assertThat(processInstance).isWaitingAt("CheckPaymentUserTask");
		
		complete(task(), withVariables("errorResolved", false));
		
		assertThat(processInstance).isWaitingAt("CreditRestoredEvent", "RestoreCreditTask").externalTask().hasTopicName("creditRestore");
		
		complete(externalTask());
		
	    assertThat(processInstance).isWaitingAt("PaymentFailedEvent").externalTask().hasTopicName("paymentCompletion");
	    
	    complete(externalTask());
		
		assertThat(processInstance).isEnded().hasPassed("PaymentFailedEvent");
	    
	}

}
