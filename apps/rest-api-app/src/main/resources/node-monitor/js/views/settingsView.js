// js/views/settingsView.js
App.Views = App.Views || {};

App.Views.SettingsView = class {
    constructor(reportView, storageService, dataService, onSettingsChangedCallback) {
        this.reportView = reportView;
        this.storageService = storageService;
        this.dataService = dataService;
        this.onSettingsChanged = onSettingsChangedCallback;
        this.#initialize();
    }

    toggleSettingsMenu() {
        const settingsPanel = document.getElementById("settingsPanel");
        const isCurrentlyHidden = settingsPanel.style.display === "none";
        settingsPanel.style.display = isCurrentlyHidden ? "block" : "none";
        if (isCurrentlyHidden) {
            this.#initializeHostsTextAndPortsText();
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #initialize() {
        this.#initializeHamburgerButton();
        this.#initializeFetchRemoteListButton();
        this.#initializeSaveConfigButton();
        this.#initializePlaceHolder();
        this.#initializeHostsTextAndPortsText();
    }

    #initializePlaceHolder() {
        document.getElementById("hostListInput").placeholder = App.Constants.PLACEHOLDER_HOST_LIST;
        document.getElementById("portListInput").placeholder = App.Constants.PLACEHOLDER_PORT_LIST;
    }

    #initializeHostsTextAndPortsText() {
        const hostsText = this.storageService.getHostsCookie();
        const portsText = this.storageService.getPortsCookie();
        document.getElementById("hostListInput").value = hostsText;
        document.getElementById("portListInput").value = portsText;
    }

    #initializeHamburgerButton() {
        document.getElementById("hamburgerButton").addEventListener("click", () => {
            this.toggleSettingsMenu();
        });
    }

    #initializeFetchRemoteListButton() {
        const fetchRemoteListButton = document.getElementById("fetchRemoteListButton");
        fetchRemoteListButton.classList.add("button", "button--orange");
        fetchRemoteListButton.addEventListener("click", async () => {
            await this.#fetchAndDisplayRemoteHostList();
        });
    }

    #initializeSaveConfigButton() {
        const saveConfigButton = document.getElementById("saveConfigButton");
        saveConfigButton.classList.add("button", "button--green");
        saveConfigButton.addEventListener("click", () => {
            this.#saveCurrentConfiguration();
        });
    }

    async #fetchAndDisplayRemoteHostList() {
        this.reportView.clearMessage();
        try {
            const remoteHosts = await this.dataService.fetchHostList();
            if (remoteHosts && Array.isArray(remoteHosts)) {
                const newValue = remoteHosts.join('\n');
                if (newValue.length > 0) {
                    document.getElementById("hostListInput").value = newValue;
                } else {
                    this.reportView.renderErrorMessage("Received host list is empty.");
                }
            } else {
                this.reportView.renderErrorMessage("Failed to fetch remote host list.");
            }
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }

    #saveCurrentConfiguration() {
        const hostsInput = document.getElementById("hostListInput").value;
        const portsInput = document.getElementById("portListInput").value;
        try {
            this.storageService.saveHostsAndPorts(hostsInput, portsInput);
            this.reportView.renderInfoMessage("Saved successfully", 1);
            this.toggleSettingsMenu();
            Promise.resolve().then(() => this.onSettingsChanged());
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }
};