const CustomerService = require("../services/customers.service");
const OrderService = require("../services/order.service");
const OrderRequest = require("../models/order.request");

module.exports.getCustomers = async (req, res, next) => {
    const users = CustomerService.getAllCustomers();
    res.status(200).json(users);
};

module.exports.getOneCustomer = (req, res, next) => {
    const customerId = parseInt(req.params.id, 10);
    const user = CustomerService.getOneCustomer(customerId);
    if (!user || user == null || user === undefined) {
        res.status(404).json({
            status: 404,
            message: `Customer with id ${customerId} not found!`
        });
    } else {
        res.status(200).json(user);
    }

};

module.exports.createOrder = async (req, res, next) => {
    const newOrderRequest = new OrderRequest(101, "Second order",
        "This is the second order, which follows the first one.");

    const orderResponse = await OrderService.createOrder(newOrderRequest);

    res.status(201).json(orderResponse);
};
