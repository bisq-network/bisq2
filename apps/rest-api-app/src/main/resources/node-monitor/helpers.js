// Helper functions

function setHostsCookie(rawHostListText) {
    const date = new Date();
    date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
    const expires = "expires=" + date.toUTCString();
    document.cookie = `hosts=${encodeURIComponent(rawHostListText)}; ${expires}; path=/; SameSite=None; Secure`;
}

function getHostsCookie() {
    const name = "hosts=";
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookieArray = decodedCookie.split(';');

    for (let i = 0; i < cookieArray.length; i++) {
        let cookie = cookieArray[i].trim();
        if (cookie.indexOf(name) === 0) {
            return cookie.substring(name.length);
        }
    }
    return "";
}

function setPortsCookie(portList) {
    const date = new Date();
    date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
    const expires = "expires=" + date.toUTCString();
    document.cookie = `ports=${encodeURIComponent(JSON.stringify(portList))}; ${expires}; path=/; SameSite=None; Secure`;
}

function getPortsCookie() {
    const name = "ports=";
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookiesArray = decodedCookie.split(';');
    for (let cookie of cookiesArray) {
        cookie = cookie.trim();
        if (cookie.indexOf(name) === 0) {
            return JSON.parse(cookie.substring(name.length));
        }
    }
    return [];
}

function formatColumnName(name) {
    return name.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
}

function parseHostListInput(input) {
    const uniqueHosts = new Set();
    input.split(/\r?\n/)
        .forEach(line => {
            line = line.trim();

            if (line.startsWith('#') || line === '') return;

            line.split(',')
                .map(host => host.trim())
                .filter(host => host)
                .forEach(host => uniqueHosts.add(host));
        });
    return Array.from(uniqueHosts);
}

function isValidHostFormat(host) {
    const hostPattern = /^[a-zA-Z0-9.-]+:\d+$/;
    return hostPattern.test(host);
}

function getFilteredHostList() {
    const hostList = parseHostListInput(document.getElementById("hostListInput").value);
    const portList = parseHostListInput(document.getElementById("portListInput").value).map(Number);

    if (portList.length === 0) {
        return hostList;
    }

    return hostList.filter(host => {
        const [, port] = host.split(':');
        return portList.includes(Number(port));
    });
}

