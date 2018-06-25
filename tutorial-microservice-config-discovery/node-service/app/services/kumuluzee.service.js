const path = require("path");
const configurationPath = path.join(__dirname, "..", "..", "config", "config.yaml");
const KumuluzeeDiscovery = require("@kumuluz/kumuluzee-discovery").default;
const ConfigurationUtil = require("@kumuluz/kumuluzee-config").ConfigurationUtil;
const remoteConfig = require("./config.bundle");

let util = null;

class KumuluzeeService {

    static getUtil () {
        return util;
    }

    static async registerService () {
        await remoteConfig.initialize({
            extension: "consul",
            configPath: configurationPath
        });
        util = ConfigurationUtil;
        await KumuluzeeDiscovery.initialize({
            extension: "consul"
        });
        KumuluzeeDiscovery.registerService();
    }

    static discoverService (serviceName, serviceVersion, environment) {
        return KumuluzeeDiscovery.discoverService({
            value: serviceName,
            version: serviceVersion,
            environment: environment,
            accessType: "DIRECT"
        });
    }

}

module.exports = KumuluzeeService;
