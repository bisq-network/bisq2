// js/controllers/appController.js
App.Controllers = App.Controllers || {};

App.Controllers.AppController = class {
    constructor(dataService, storageService) {
        this.dataService = dataService;
        this.storageService = storageService;
        this.reportView = new App.Views.ReportView();
        this.settingsView = new App.Views.SettingsView(
            this.reportView,
            this.storageService,
            this.dataService,
            this.onSettingsChanged.bind(this)
        );
    }

    async initApp() {
        try {
            await this.#updateHostsAndLoadAllReports();
            this.#setInitialUIState();

            this.#initReloadButton();
            this.#initToggleAllButton();
        } catch (error) {
            console.error("Error on init app", error);
            this.reportView.renderErrorMessage(error.message);
        }
    }

    async onSettingsChanged() {
        try {
            await this.#updateHostsAndLoadAllReports();
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }

    //////////////////////
    // PRIVATE METHODS
    //////////////////////

    #setInitialUIState() {
        const hasHosts = this.hostList && this.hostList.length > 0;
        this.#toggleReloadButton(hasHosts);
    }

    #initReloadButton() {
        document.getElementById("reloadButton").addEventListener("click", () => {
            if (this.hostList.length > 0) {
                this.#loadAllReports();
            } else {
                this.reportView.renderErrorMessage(App.Constants.STATUS_ENTER_HOSTS);
            }
        });
    }

    #toggleReloadButton(show) {
        this.#toggleButton(show, "reloadButton");
    }

    #initToggleAllButton() {
        document.getElementById("toggleAllButton").addEventListener("click", () => {
            const expand = document.getElementById("toggleAllButton").textContent === App.Constants.BUTTON_EXPAND_ALL;
            const toggleButtons = document.querySelectorAll('.toggle-button');
            toggleButtons.forEach(button => {
                try {
                    if (button.dataset.details) {
                        this.reportView.toggleDetailButton(button, expand);
                    }
                } catch (error) {
                    console.warn("Skipping button due to invalid JSON in dataset.value:", button);
                }
            });

            document.getElementById("toggleAllButton").textContent = expand ?
                App.Constants.BUTTON_COLLAPSE_ALL :
                App.Constants.BUTTON_EXPAND_ALL;
        });
    }

    #toggleToggleAllButton(show) {
        this.#toggleButton(show, "toggleAllButton");
    }

    #toggleButton(show, elementById) {
        const button = document.getElementById(elementById);
        button.style.display = show ? "block" : "none";
    }

    async #getHosts() {
        let hosts = this.storageService.getHostsFromCookie();
        if (hosts.length === 0) {
            hosts = await this.dataService.fetchHostList();
            if (hosts && hosts.length > 0) {
                this.storageService.saveHosts(hosts.join('\n'));
                this.reportView.clearMessage();
            } else {
                throw new Error("No hosts available from server.");
            }
        }
        return hosts;
    }

    #cleanupObsoleteReports() {
        const existingHostElements = document.querySelectorAll('.node-block');
        existingHostElements.forEach(element => {
            const hostId = element.id.replace('report-', '').replace(/-/g, ':');
            const hostPort = parseInt(hostId.split(":")[1]);

            if (!this.hostList.includes(hostId) || (this.portList && !this.portList.includes(hostPort))) {
                element.remove();
                console.log(`Removed obsolete or filtered host element for: ${hostId}`);
            }
        });
    }

    async #updateHostsAndLoadAllReports() {
        try {
            this.hostList = this.storageService.getHostsFromCookie();
            this.portList = this.storageService.getPortsFromCookie();

            if (this.hostList.length === 0) {
                this.hostList = await this.#getHosts();
                if (this.hostList.length === 0) {
                    this.reportView.renderErrorMessage(App.Constants.STATUS_ENTER_HOSTS);
                    this.#toggleReloadButton(false);
                    this.#toggleToggleAllButton(false);
                    return;
                }
            } else {
                this.#toggleReloadButton(true);
                this.#toggleToggleAllButton(true);
            }

            this.#cleanupObsoleteReports();
            this.#loadAllReports();
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
            this.#toggleReloadButton(false);
            this.#toggleToggleAllButton(false);
        }
    }

    #createOrUpdatePlaceholderForReport(host) {
        const existingNodeBlock = document.getElementById(`report-${host.replace(/[:.]/g, '-')}`);
        if (!existingNodeBlock) {
            const nodeBlock = document.createElement('div');
            nodeBlock.classList.add('node-block');
            nodeBlock.id = `report-${host.replace(/[:.]/g, '-')}`;

            const header = document.createElement('div');
            header.classList.add('node-header');

            const statusCircle = document.createElement('span');
            statusCircle.classList.add('status-circle', 'status-loading');
            header.appendChild(statusCircle);

            const hostText = document.createElement('span');
            hostText.textContent = host;
            header.appendChild(hostText);

            nodeBlock.appendChild(header);
            document.getElementById("reportContainer").appendChild(nodeBlock);
        } else {
            const statusCircle = existingNodeBlock.querySelector('.status-circle');
            statusCircle.className = 'status-circle status-loading';
        }
    }

    async #loadSingleReport(host) {
        try {
            const { data } = await this.dataService.fetchReportData(host);
            this.reportView.renderSingleReport(data, host);
        } catch (error) {
            this.reportView.renderErrorMessage(error.message);
        }
    }

    #loadAllReports() {
        console.log("Loading reports for hosts:", this.hostList);
        console.log("Allowed Ports:", this.portList);

        if (this.hostList.length > 0) {
            this.#toggleToggleAllButton(true);
            this.hostList.forEach(host => {
                const hostPort = parseInt(host.split(":")[1]);
                console.log("Checking host:", host, "with extracted port:", hostPort);

                if (!this.portList || this.portList.length === 0 || this.portList.includes(hostPort)) {
                    console.log("Fetching report for host with port:", host);
                    this.#createOrUpdatePlaceholderForReport(host);
                    this.#loadSingleReport(host);
                } else {
                    console.log("Skipping host as port not in filter list:", host);
                }
            });
        } else {
            this.#toggleToggleAllButton(false);
        }
    }
};
