Medicine Availability Finder System

A web-based project designed to help users find medicine availability at pharmacies, manage pharmacy stock, and analyze medicine inventory using a Python analytics dashboard.

Project Overview

The Medicine Availability Finder System combines a Java-based web application with a Python-based analytics dashboard. It connects pharmacy stock information with medicine searching, stock management, and data analysis.

The project also implements Design and Analysis of Algorithms (DAA) techniques to search medicines, sort pharmacy offers, and recommend the lowest-priced available option.

Project Objectives

- Help users search for medicines available at pharmacies.
- Display pharmacy details, medicine quantities, and prices.
- Allow customers to register and log in.
- Enable pharmacy owners to manage medicine stock.
- Provide stock availability and inventory reports.
- Analyze medicine stock using Python.
- Demonstrate DAA algorithms using real medicine-stock data.

Main Features

1. Java Web Application

- Medicine search by medicine name and city.
- Customer registration and login.
- Customer dashboard and logout.
- Pharmacy owner registration and login.
- Pharmacy stock management.
- Add and update medicine stock.
- Medicine availability and quantity display.
- Medicine request functionality.
- Admin-related pharmacy management.

2. Python Medicine Stock Analytics Dashboard

The Python dashboard reads medicine-stock data from the MySQL database and generates analytical reports.

Features include:

- Total stock records.
- In-stock and out-of-stock records.
- Low-stock alerts for quantities between 1 and 5 units.
- Total medicine quantity.
- Top medicines ranked by stock quantity.
- City-wise medicine stock analysis.
- Pharmacy stock details.
- Stock charts and visualizations.
- Search for a medicine using DAA algorithms.
- Pharmacy offers sorted by price.
- Recommendation of the cheapest eligible in-stock pharmacy.

Design and Analysis of Algorithms (DAA)

The Python dashboard implements the following algorithms.

1. Binary Search

Function: "binary_search_medicine()"

Binary Search finds a medicine in a sorted list of unique medicine names. It repeatedly divides the search interval into two halves until the medicine is found or no candidates remain.

- Purpose: Search for a medicine.
- Input: Sorted medicine names and the target medicine.
- Output: Index of the medicine, or "-1" if not found.
- Time Complexity: O(log n).
- Space Complexity: O(1) auxiliary space for the iterative search.

2. Merge Sort

Function: "merge_sort_pharmacies()"

Merge Sort sorts pharmacy offers by price in ascending order using the Divide and Conquer technique. Missing or invalid prices are placed after valid prices.

- Purpose: Arrange pharmacy offers from lowest to highest price.
- Input: Pharmacy stock records.
- Output: Sorted pharmacy offers.
- Time Complexity: O(n log n).
- Space Complexity: O(n) auxiliary space for merging.

3. Greedy Algorithm

Function: "greedy_cheapest_available()"

The Greedy algorithm examines eligible pharmacy offers and selects the offer with the minimum valid price among pharmacies with available stock.

- Purpose: Recommend the lowest-priced eligible pharmacy.
- Input: Pharmacy offers containing quantity and price.
- Output: The cheapest eligible offer, or "None" if no eligible offer exists.
- Time Complexity: O(n) for examining the offers and selecting the minimum.
- Approach: Greedy selection based on minimum price.

Note: The recommendation considers price and stock availability only. It is not a medical recommendation.

Technology Stack

Java Application

- Java
- Jakarta Servlets
- Maven
- JDBC
- MySQL
- HTML
- CSS
- JavaScript
- Apache Tomcat

Python Analytics Dashboard

- Python
- Flask
- MySQL Connector/Python
- Pandas
- Matplotlib
- HTML
- CSS

Database

Database Name: "medicine_finder"

The application uses MySQL to store and retrieve medicine, pharmacy, customer, owner, stock, and related application data.

The Python dashboard retrieves stock information using SQL queries and processes it for analysis and visualization.

Repository Structure

Medicine-Availability-Finder-System/
│
├── src/
│   └── Java application source code
│
├── pom.xml
├── .gitignore
├── README.md
│
└── python-dashboard/
    ├── app.py
    ├── medicine_stock_analytics.py
    ├── medicine_stock_report.csv
    └── Chart images and related files

The exact files may vary depending on the current project version.

Installation and Setup

Requirements

Install the following software before running the project:

- A compatible JDK.
- Apache NetBeans or another Java IDE.
- A compatible Apache Tomcat version.
- MySQL Server.
- Python 3.
- Required Python packages.

Running the Java Application

1. Clone or download this repository.
2. Open the Java project in NetBeans.
3. Create the MySQL database named "medicine_finder".
4. Import the database SQL file supplied separately.
5. Configure the database connection in the application's database connection class.
6. Add the required MySQL JDBC driver.
7. Configure a compatible Tomcat server.
8. Clean and build the project.
9. Run the web application.

Running the Python Dashboard

1. Open a terminal or Command Prompt.

2. Navigate to the "python-dashboard" directory.

3. Install the required Python packages:
   
   python -m pip install flask mysql-connector-python pandas matplotlib

4. Ensure that MySQL Server is running and the "medicine_finder" database is available.

5. Ensure the database contains the required tables and stock data.

6. Run the dashboard:
   
   python app.py

7. Enter the MySQL root password when prompted.

8. Open the following address in your browser:
   
   "http://127.0.0.1:5000"

Keep the terminal open while using the dashboard.

Security Notes

- Never upload database passwords, private credentials, or secret keys to GitHub.
- Configure database credentials locally.
- Keep virtual environments, cache files, and unnecessary build output out of the repository.
- The database SQL export is maintained separately from the repository.

Learning Outcomes

Through this project, the following concepts are demonstrated:

- Java web application development.
- Database connectivity using JDBC and MySQL.
- User authentication and session management.
- Pharmacy stock management.
- Python web development using Flask.
- Data processing using Pandas.
- Data visualization using Matplotlib.
- Implementation of Binary Search, Merge Sort, and a Greedy algorithm.
- Time and space complexity analysis.
- Integration of database data with algorithms and analytics.

Conclusion

The Medicine Availability Finder System combines medicine searching, pharmacy stock management, and Python-based inventory analytics in one project. It demonstrates the practical application of web development, database management, data visualization, and DAA algorithms.

---

Project: Medicine Availability Finder System

Project Type: College Academic Project
