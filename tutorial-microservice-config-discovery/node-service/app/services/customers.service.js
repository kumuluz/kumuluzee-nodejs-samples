const Customer = require("../models/customer.model");

const mockDB = [
    new Customer(100, "John", "Carlile", "john.ca@mail.com", "053347863"),
    new Customer(101, "Ann", "Lockwood", "lockwood_ann@mail.com", "023773123"),
    new Customer(102, "Elizabeth", "Mathews", "eli23@mail.com", "043343403"),
    new Customer(103, "Isaac", "Anderson", "isaac.anderson@mail.com", "018743831"),
    new Customer(104, "Barret", "Peyton", "barretp@mail.com", "063343148"),
    new Customer(105, "Terry", "Cokes", "terry_cokes@mail.com", "053339123")
];

class CustomerService {

    static getAllCustomers () {
        return mockDB;
    }

    static getOneCustomer (id) {
        if (!Number.isInteger(id)) {
            throw new Error("id must be integer!");
        }
        return mockDB.find((user) => {
            return user.id === id;
        });
    }

}

module.exports = CustomerService;
