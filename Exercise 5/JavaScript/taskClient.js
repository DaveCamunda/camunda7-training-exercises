const { Client, logger } = require("camunda-external-task-client-js");
const { Variables } = require("camunda-external-task-client-js");

const config = { baseUrl: "http://localhost:8080/engine-rest",
                 use: logger,
                 maxTasks: 1};

const client = new Client(config);

client.subscribe("creditDeduction", async function({ task, taskService }) {
  const credit = Number(task.variables.get("customerCredit"));

  console.log("Customer's credit: " + credit);
  console.log("Credit deduction finished");

  await taskService.complete(task);

});

client.subscribe("creditCardCharging", async function({ task, taskService }) {
  console.log("Charging card...");
  console.log("Charged the customer's card");

  await taskService.complete(task);

});
