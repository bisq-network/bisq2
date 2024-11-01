// js/services/storageService.js
App.Services = App.Services || {};

App.Services.StorageService = class {
    constructor() {
        this.hostsKey = App.Constants.HOSTS_COOKIE_KEY;
        this.portsKey = App.Constants.PORTS_COOKIE_KEY;
    }

    saveHostsAndPorts(hostsInput, portsInput) {
        try {
            const hostList = this.#parseHostListInput(hostsInput);
            const portList = this.#parsePortListInput(portsInput);

            this.#setHostsCookie(hostsInput);
            this.#setPortsCookie(portsInput);
        } catch (error) {
            throw new Error("Host and port lists not stored: " + error.message);
        }
    }

    saveHosts(hostsInput) {
        try {
            const hostList = this.#parseHostListInput(hostsInput);

            this.#setHostsCookie(hostsInput);
        } catch (error) {
            throw new Error("Host list not stored: " + error.message);
        }
    }

    getHostsFromCookie() {
        const hostsText = this.getHostsCookie();
        const uniqueHosts = new Set();

        hostsText.split(/\r?\n/).forEach(line => {
            line = line.trim();
            if (line && !line.startsWith('#')) {
                line.split(',').forEach(host => {
                    host = host.trim();
                    if (host) uniqueHosts.add(host);
                });
            }
        });

        return Array.from(uniqueHosts);
    }

    getPortsFromCookie() {
        const portsText = this.getPortsCookie();
        const uniquePorts = new Set();

        portsText.split(/\r?\n/).forEach(line => {
            line = line.trim();
            if (line && !line.startsWith('#')) {
                line.split(',').forEach(port => {
                    port = port.trim();
                    if (port && !isNaN(port)) uniquePorts.add(Number(port));
                });
            }
        });

        return Array.from(uniquePorts);
    }

    getHostsCookie() {
        return this.#getCookie(this.hostsKey) || "";
    }

    getPortsCookie() {
        return this.#getCookie(this.portsKey) || "";
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #getCookie(name) {
        const decodedCookie = decodeURIComponent(document.cookie);
        const cookiesArray = decodedCookie.split(';');
        for (let cookie of cookiesArray) {
            cookie = cookie.trim();
            if (cookie.startsWith(name + "=")) {
                return cookie.substring(name.length + 1);
            }
        }
        return null;
    }

    #parseHostListInput(input) {
        return input
            .split(/\r?\n|,/)
            .map(line => line.trim())
            .filter(line => line && !line.startsWith('#'))
            .map(item => {
                if (!this.#isValidHostFormat(item)) {
                    throw new Error(`Invalid host entry detected: "${item}"`);
                }
                return item;
            });
    };

    #isValidHostFormat = (host) => {
        const hostPattern = /^[a-zA-Z0-9.-]+:\d+$/;
        return hostPattern.test(host);
    };
    
    #parsePortListInput(input) {
        return input
            .split(/\r?\n|,/) 
            .map(line => line.trim()) 
            .filter(line => line && !line.startsWith('#'))
            .map(item => {
                const port = Number(item);
                if (isNaN(port) || port <= 0 || port > 65535) {
                    throw new Error(`Invalid or out-of-range port detected: "${item}"`);
                }
                return port;
            });
    };

    #setCookie(key, text) {
        const date = new Date();
        date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
        const expires = "expires=" + date.toUTCString();
        document.cookie = `${key}=${encodeURIComponent(text)}; ${expires}; path=/; SameSite=None; Secure`;
    }

    #setHostsCookie(hostsText) {
        this.#setCookie(this.hostsKey, hostsText);
    }

    #setPortsCookie(portsText) {
        this.#setCookie(this.portsKey, portsText);
    }
};

