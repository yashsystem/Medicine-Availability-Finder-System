import pandas as pd
import matplotlib.pyplot as plt
import mysql.connector

# Connect to the existing Medicine Finder database
try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password=input("Enter your MySQL password: "),
        database="medicine_finder"
    )

    print("\nDatabase connected successfully!")

    query = """
        SELECT
            m.name AS medicine,
            m.strength AS strength,
            p.name AS pharmacy,
            p.city AS city,
            s.quantity AS quantity,
            s.price AS price
        FROM pharmacy_stock s
        JOIN medicines m ON s.medicine_id = m.id
        JOIN pharmacies p ON s.pharmacy_id = p.id
        ORDER BY s.quantity ASC
    """

    df = pd.read_sql(query, conn)

    if df.empty:
        print("No stock records found in the database.")
    else:
        # Display stock records
        print("\nMEDICINE STOCK REPORT")
        print("=" * 70)
        print(df.to_string(index=False))

        # Summary statistics
        print("\nSTOCK SUMMARY")
        print("=" * 70)
        print("Total stock records:", len(df))
        print("Total units in stock:", int(df["quantity"].sum()))
        print("Available records:", int((df["quantity"] > 0).sum()))
        print("Out-of-stock records:", int((df["quantity"] == 0).sum()))
        print("Low-stock records (1-5 units):",
              int(((df["quantity"] >= 1) &
                   (df["quantity"] <= 5)).sum()))

        # Save report to CSV
        df.to_csv("medicine_stock_report.csv", index=False)
        print("\nReport saved as medicine_stock_report.csv")

        # Graph 1: Stock quantity by pharmacy
        pharmacy_stock = df.groupby("pharmacy")["quantity"].sum()

        pharmacy_stock.plot(kind="bar")
        plt.title("Total Medicine Stock by Pharmacy")
        plt.xlabel("Pharmacy")
        plt.ylabel("Total Quantity")
        plt.xticks(rotation=30, ha="right")
        plt.tight_layout()
        plt.savefig("pharmacy_stock_chart.png")
        plt.show()

        # Graph 2: Stock status
        available = int((df["quantity"] > 0).sum())
        out_of_stock = int((df["quantity"] == 0).sum())

        plt.figure()
        plt.pie(
            [available, out_of_stock],
            labels=["In Stock", "Out of Stock"],
            autopct="%1.1f%%",
            startangle=90
        )
        plt.title("Medicine Stock Status")
        plt.tight_layout()
        plt.savefig("medicine_stock_status.png")
        plt.show()

        print("\nAnalytics completed successfully!")
        print("Charts saved as PNG files.")

    conn.close()

except mysql.connector.Error as e:
    print("\nMySQL connection/database error:", e)
except Exception as e:
    print("\nError:", e)