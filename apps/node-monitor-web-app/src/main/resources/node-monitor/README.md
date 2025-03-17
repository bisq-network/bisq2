
# Bisq Node Monitor Application

## Overview

The **Bisq Node Monitor** is a web application designed to monitor Bisq nodes. 
The application provides a user interface to input a list of hosts and ports, retrieves their status from an API, and displays the results in a structured and interactive format.

## Project Structure

The project is organized into different modules to ensure a clear separation of concerns and ease of maintenance. Each part of the application has a dedicated file or directory, as outlined below:

```
project-root/
│
├── index.html                   # Main HTML file defining the application's structure
├── index.js                     # Main JavaScript file for initializing the application
├── README.md                    # Project documentation
│
├── js/                          # JavaScript files organized by functionality
│   ├── Constants.js             # Global constants used throughout the application
│   ├── controllers/             # Controllers for managing application logic
│   │   ├── AppController.js     # Main controller for user interaction and API calls
│   │   ├── ReportController.js  # Controller for handling reports
│   │   └── ReportDiffsController.js # Controller for report comparison and analysis
│   ├── errors.js                # Error handling logic
│   ├── services/                # Services for data and storage management
│   │   ├── DataService.js       # Service for managing API requests and data retrieval
│   │   └── StorageService.js    # Service for handling local storage
│   ├── utils/                   # Utility functions
│   │   ├── DOMUtils.js          # Functions for DOM manipulation
│   │   ├── FormatUtils.js       # Formatting utilities
│   │   └── KeyUtils.js          # Utilities for key management
│   └── views/                   # View components for different sections
│       ├── ReportView.js        # View for displaying reports
│       └── SettingsView.js      # View for handling settings
│
├── resources/                   # Resources such as images and icons
│   └── Bisq2_icon.svg           # Application icon
│
└── styles/                      # CSS files for styling
    ├── global.css               # Global styles, typography, colors, and basic element styling
    ├── page-layout.css          # Layout and positioning of main areas, responsive styles
    ├── reportView.css           # Styling for the report view section
    └── settingsView.css         # Styling for the settings view section
```

## Description

Here’s a revised version integrating your requirements:

ReportDiffs enables comparison across all reports and highlights deviations.
It provides average values and supports three configurable thresholds, which 
can be adjusted in the settings. Threshold violations are visually marked, 
with a hover showing the specific average value and the extent of the deviation.
For subtables, the background of the button toggling the detail view is marked
based on the highest exceeded threshold.

For certain metrics, deviations are highlighted based on fixed reference values
rather than averages:
    numConnections: Marked as critical if below 10, highlighting downward deviations.
    memoryUsed: Marked as critical if exceeding 500 MB, highlighting upward deviations.
    numThreads: Marked as critical if exceeding 70, highlighting upward deviations.
    nodeLoad: Marked as critical if exceeding 0.4, highlighting upward deviations.
