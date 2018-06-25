const ConfigBundle = require("@kumuluz/kumuluzee-config").default;

const remoteConfig = new ConfigBundle({
    prefixKey: "rest-config",
    type: "object",
    fields: {
        maintenanceMode: {
            type: "boolean",
            name: "maintenance",
            watch: true
        }
    }
});

module.exports = remoteConfig;
