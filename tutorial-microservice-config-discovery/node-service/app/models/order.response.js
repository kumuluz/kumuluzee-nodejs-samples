
class OrderResponse {

    constructor (data) {
        this.id = data.id;
        this.title = data.title;
        this.description = data.description;
        this.customerId = data.customerId;
    }

}

module.exports = OrderResponse;
