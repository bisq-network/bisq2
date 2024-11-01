// index.js
// Namespace for the application
window.App = window.App || {};

document.addEventListener("DOMContentLoaded", () => {
    const dataService = new App.Services.DataService();
    const storageService = new App.Services.StorageService();

    const appController = new App.Controllers.AppController(dataService, storageService);
    appController.initApp();
});
