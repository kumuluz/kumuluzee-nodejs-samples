const axios = require("axios");
const KumuluzeeService = require("./kumuluzee.service");
const OrderResponse = require("../models/order.response");

class OrderService {

    static async createOrder (orderRequest) {
        const serviceUrl = await KumuluzeeService.discoverService("java-service", "1.0.0", "dev");
        const apiUrl = `${serviceUrl}v1/orders`;
        try {
            const responseObject = await axios.post(apiUrl, orderRequest);
            if (responseObject.status === 201) {
                return new OrderResponse(responseObject.data);
            } else {
                throw new Error(`Bad response code: ${responseObject.status}!`);
            }
        } catch (requestException) {
            console.error("Napaka pri POST zahtevku! ", requestException);
        }
    }

}

module.exports = OrderService;
