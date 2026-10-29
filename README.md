# Medicine Availability Finder System

A college project that helps users find medicine availability at pharmacies and provides medicine stock analytics.

## Project Overview

The Medicine Availability Finder System combines a Java-based web application with a Python analytics dashboard.

### 1. Java Web Application
- Search for medicines and view pharmacy stock.
- Customer registration and login.
- Pharmacy owner login and stock management.
- Medicine request and stock status features.
- Admin-related pharmacy management.

### 2. Python Analytics Dashboard
- Medicine stock analysis.
- Pharmacy stock reports in CSV format.
- Stock charts and visualizations.
- City-wise and stock-status visualizations.

## Technology Stack

**Java Application**
- Java
- Servlets / Jakarta EE
- Maven
- MySQL
- HTML and CSS

**Python Analytics**
- Python
- Pandas / data analysis libraries (as used in the code)
- CSV reports
- Data visualization

## Repository Structure

```text
Medicine-Availability-Finder-System/
├── src/
├── pom.xml
├── .gitignore
└── python-dashboard/
    ├── app.py
    ├── app_backup.py
    ├── medicine_stock_analytics.py
    ├── medicine_stock_report.csv
    └── chart images
```

## Setup

### Java Application
1. Install a compatible JDK, Maven support, Apache Tomcat, and MySQL.
2. Import the project into NetBeans or another compatible IDE.
3. Create the `medicine_finder` database.
4. Import the database SQL file.
5. Configure the database connection locally.
6. Build and run the web application on the configured server.

### Python Dashboard
1. Install Python 3.
2. Open the `python-dashboard` folder.
3. Install the dependencies required by the Python scripts.
4. Run the appropriate Python file according to its instructions.

## Important Notes
- Database credentials should be configured locally and must not be committed to GitHub.
- The SQL database export is maintained separately from this repository.
- The Python dashboard requires its appropriate dependencies and input data.

## Project Purpose

This project demonstrates web development, database connectivity, pharmacy stock management, and Python-based data analysis.

## Author

Student College Project
