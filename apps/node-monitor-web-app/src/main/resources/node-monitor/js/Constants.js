export class Constants {

    static MODE_DEV = 'dev';
    static MODE_PROD = 'prod';

    static STATUS_ERROR = "Failed to fetch data";
    static STATUS_ENTER_ADDRESSES = "Please enter an address list in the settings to start fetching data.";
    static PLACEHOLDER_ADDRESS_LIST =
        "Host:port list in host:port format, separated by commas or new lines.\n# Comments and empty lines are allowed.";
    static PLACEHOLDER_PORT_LIST =
        "Port list, for filtering addresses. Separated by commas or new lines.\n# Comments and empty lines are allowed.";

    static BUTTON_EXPAND_ALL = "Expand All Details";
    static BUTTON_COLLAPSE_ALL = "Collapse All Details";
    static BUTTON_EXPAND_DETAILS = "Expand Details";
    static BUTTON_COLLAPSE_DETAILS = "Collapse Details";

    static SELECTOR_DATA_KEY = 'data-key';
    static SELECTOR_DATA_FULL_KEY = 'data-fullkey';

    static REPORT_KEY_ROOT_PARENT = '';
    static REPORT_KEY_ROOT = "Report";
    static HIERARCHY_DELIMITER = '.';

    static DEVIATION_THRESHOLDS_LOW = 5;
    static DEVIATION_THRESHOLDS_MEDIUM = 20;
    static DEVIATION_THRESHOLDS_HIGH = 50;
    static DEFAULT_DEVIATION_THRESHOLDS = Object.freeze({
        LOW: { value: Constants.DEVIATION_THRESHOLDS_LOW },
        MEDIUM: { value: Constants.DEVIATION_THRESHOLDS_MEDIUM },
        HIGH: { value: Constants.DEVIATION_THRESHOLDS_HIGH }
    });
    static CONFIG_KEY = Object.freeze({
        ADDRESSES_COOKIE: 'addresses',
        PORTS_COOKIE: 'ports',
        DEVIATION_THRESHOLDS: 'deviation_thresholds'
    });
    static DEVIATION_THRESHOLDS = {};

    // TODO: Reorg to Configs
    static API_URL_GET_REPORT = null;
    static API_URL_GET_ADDRESSES = null;
    static API_URL_GET_ADDRESSES_DETAILS = null;
    static QUERY_PARAM_ADDRESSES = 'addresses';

    static MODE = null;

    static initialize(savedConfig = {}) {
        const validateThreshold = (value, defaultValue) => {
            return typeof value === 'number' && value > 0 ? value : defaultValue;
        };

        this.MODE = savedConfig.MODE || this.MODE_PROD;

        this.DEVIATION_THRESHOLDS = Object.freeze({
            LOW: { value: validateThreshold(savedConfig.DEVIATION_THRESHOLDS?.LOW, this.DEVIATION_THRESHOLDS_LOW) },
            MEDIUM: { value: validateThreshold(savedConfig.DEVIATION_THRESHOLDS?.MEDIUM, this.DEVIATION_THRESHOLDS_MEDIUM) },
            HIGH: { value: validateThreshold(savedConfig.DEVIATION_THRESHOLDS?.HIGH, this.DEVIATION_THRESHOLDS_HIGH) }
        });

        const { protocol, hostname, port } = window.location;
        const baseURL = `${protocol}//${hostname}${port ? `:${port}` : ''}`;

        this.API_URL_GET_REPORT = `${baseURL}/api/v1/report`;
        this.API_URL_GET_ADDRESSES = `${baseURL}/api/v1/report/addresses`;
        this.API_URL_POST_ADDRESSES_DETAILS = `${baseURL}/api/v1/report/addresses/details`;

        console.log('Constants initialized with:', {
            MODE: this.MODE,
            DEVIATION_THRESHOLDS: this.DEVIATION_THRESHOLDS,
            API_URL_GET_REPORT: this.API_URL_GET_REPORT,
            API_URL_GET_ADDRESSES: this.API_URL_GET_ADDRESSES,
            API_URL_GET_ADDRESSES_DETAILS: this.API_URL_GET_ADDRESSES_DETAILS
        });
    }
}
