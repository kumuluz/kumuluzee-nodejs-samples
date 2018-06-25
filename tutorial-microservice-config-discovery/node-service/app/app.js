const express = require("express");
const cookieParser = require("cookie-parser");
const bodyParser = require("body-parser");
const CustomerRouter = require("./routes/customers.route");
const remoteConfig = require("./services/config.bundle");

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));
app.use(cookieParser());

app.use(async (req, res, next) => {
    const isInMaintenance = remoteConfig.maintenanceMode;
    if (isInMaintenance) {
        res.status(503);
        res.json({
            status: 503,
            message: "Service is in maintenance mode, try again later!"
        });
    } else {
        next();
    }
});

app.use("/v1/customers", CustomerRouter);

app.use((req, res, next) => {
    const err = new Error("Not found");
    err.status = 404;
    next(err);
});

app.use((err, req, res, next) => {
    const status = err.status || 500;
    const responseObject = {
        status: status,
        message: err.message,
        stackTrace: req.app.get("env") === "development" ? err.stack : {}
    };
    res.status(status);
    res.json(responseObject);
});

module.exports = app;
