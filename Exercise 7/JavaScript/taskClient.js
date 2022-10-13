const { Client, logger } = require("camunda-external-task-client-js");
const { Variables } = require("camunda-external-task-client-js");
const axios = require('axios');

const config = { baseUrl: "http://localhost:8080/engine-rest",
                 use: logger,
                 maxTasks: 1};

const client = new Client(config);

client.subscribe("creditDeduction", async function({ task, taskService }) {

    console.log("Deducting customer credit...");

    const customerId = task.variables.get("customerId");
    const orderTotal = Number(task.variables.get("orderTotal"));

    const customerCredit = getCustomerCredit(customerId);
    const openAmount = deductCredit(orderTotal, customerCredit);

    console.log("Charged " + customerCredit + " EUR from customer's credit. Open amount is: " + openAmount + " EUR");

    const processVariables = new Variables();

    processVariables.set("openAmount", openAmount);
    processVariables.set("customerCredit", customerCredit);

    await taskService.complete(task, processVariables, null);

});

client.subscribe("creditCardCharging", async function({ task, taskService }) {

    console.log("Charging card...");

    const cardNumber = task.variables.get("cardNumber"),
          expiryDate = task.variables.get("expiryDate"),
          amount = task.variables.get("openAmount"),
          cvc = task.variables.get("cvc");

    console.log("Charged card " + cardNumber + " that expires on " + expiryDate + " and has cvc " + cvc + " with amount of " + amount + " EUR");

    await taskService.complete(task);

});

client.subscribe("paymentRequest", async function({ task, taskService }) {

    console.log("Sending payment request message...");

    const json = JSON.stringify({ "messageName" : "paymentRequestMessage", "businessKey" : task.businessKey, "processVariables" : task.variables.getAllTyped() });

    console.log(json);

    const res = await axios.post('http://localhost:8080/engine-rest/message', json, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    await taskService.complete(task);
});

client.subscribe("paymentCompletion", async function({ task, taskService }) {

    console.log("Sending payment completion request message...");

    const json = JSON.stringify({ "messageName" : "paymentCompletedMessage", "businessKey" : task.businessKey, "processVariables" : task.variables.getAllTyped() });

    const res = await axios.post('http://localhost:8080/engine-rest/message', json, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    await taskService.complete(task);
});

function getCustomerCredit(customerId) {

    var credit = 0.0;

    const regEx = /\d+/;

    const match = customerId.match(regEx);

    if (match) { credit = parseFloat(match); }

    return credit;
}

function deductCredit(amount, credit) {

    var openAmount = 0.0;

    if (credit < amount) {
        openAmount = amount - credit;
    }

    return openAmount;
}
