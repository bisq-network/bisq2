// js/constants.js
App.Constants = {
    API_URL_GET_REPORT: 'http://localhost:8082/api/v1/report',
    API_URL_GET_ADDRESS_LIST: 'http://localhost:8082/api/v1/report/address-list',
    STATUS_ERROR: "Failed to fetch data",
    STATUS_ENTER_HOSTS: "Please enter a Host:port list in the settings to start fetching data.",
    PLACEHOLDER_HOST_LIST: "Host:port list, separated by commas or new lines.\n# Comments and empty lines are alowed.",
    PLACEHOLDER_PORT_LIST: "Port list, for filtering hosts. Separated by commas or new lines.\n# Comments and empty lines are alowed.",
    BUTTON_EXPAND_ALL: "Expand All Details",
    BUTTON_COLLAPSE_ALL: "Collapse All Details",
    BUTTON_EXPAND_DETAILS: "Expand Details",
    BUTTON_COLLAPSE_DETAILS: "Collapse Details",
    HOSTS_COOKIE_KEY: 'hosts',
    PORTS_COOKIE_KEY: 'ports',
};
