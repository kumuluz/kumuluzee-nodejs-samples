const express = require("express");
const router = express.Router();

const CustomerController = require("../controllers/customers.controller");

router.get("/", CustomerController.getCustomers);

router.get("/:id/order", CustomerController.createOrder);

router.get("/:id", CustomerController.getOneCustomer);

module.exports = router;
