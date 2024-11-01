
# Bisq Node Monitor Application

## Overview

The **Bisq Node Monitor** is a web application designed to monitor Bisq nodes. 
The application provides a user interface to input a list of hosts and ports, retrieves their status from an API, and displays the results in a structured and interactive format.

## Project Structure

The project is organized into different modules to ensure a clear separation of concerns and ease of maintenance. Each part of the application has a dedicated file or directory, as outlined below:

```
projekt-root/
│
├── index.html                   # Main HTML file that defines the application's structure
├── index.js                     # Main JavaScript file for initializing the application
├── README.md                    # Project README file with documentation
│
├── js/                          # JavaScript files organized by functionality
│   ├── constants.js             # Global constants used throughout the application
│   ├── controllers/             # Application controllers
│   │   └── appController.js     # Main application controller for handling user input and API calls
│   ├── services/                # Application services for data and storage management
│   │   ├── dataService.js       # Service handling API requests and data retrieval
│   │   └── storageService.js    # Service for handling local storage interactions
│   └── views/                   # View components for different sections
│       ├── settingsView.js      # View handling settings display and input
│       └── reportView.js        # View managing the display of node reports
│
└── styles/                      # Directory containing CSS files for styling
    ├── global.css               # Global styles, typography, colors, and basic element styling
    ├── page-layout.css          # Layout and positioning of main areas, responsive styling
    ├── reportView.css           # Styling for the report view section
    └── settingsView.css         # Styling for the settings view section
```
