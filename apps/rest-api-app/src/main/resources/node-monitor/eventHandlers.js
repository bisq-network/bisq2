// Event handlers and initialization

// Automatic loading at startup if a host list is available
window.onload = () => {
    const savedHosts = getHostsCookie();
    const savedPorts = getPortsCookie();

    if (savedHosts) {
        document.getElementById("hostListInput").value = savedHosts;
    }
    if (savedPorts && savedPorts.length > 0) {
        document.getElementById("portListInput").value = savedPorts.join('\n');
    }

    if (savedHosts.trim().length > 0) {
        loadData();
    } else {
        updateStatusMessage("Please enter a Host:port list in the settings to start fetching data.", "red");
    }
};

// Toggle Settings Panel
document.getElementById("hamburgerButton").addEventListener("click", toggleSettingsMenu);

// Save Configuration and Close Settings Panel
document.getElementById("saveConfigButton").addEventListener("click", function() {
    const rawHostListText = document.getElementById("hostListInput").value;
    const portList = parseHostListInput(document.getElementById("portListInput").value).map(Number);

    setHostsCookie(rawHostListText);
    setPortsCookie(portList);

    loadData();
    toggleSettingsMenu();
});

// Toggle all details (global expand/collapse button)
document.getElementById("toggleAllButton").addEventListener('click', function() {
    const expand = this.textContent === "Expand All Details";
    toggleAllDetails(expand);
});
