"use strict";

class Customer {

    constructor (id, name, lastName, email, phone) {
        if (!Number.isInteger(id)) {
            throw new Error("id is integer!");
        }
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

}

module.exports = Customer;
