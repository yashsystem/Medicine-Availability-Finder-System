import getpass
from pathlib import Path

from flask import Flask, render_template_string, send_file
import mysql.connector
import pandas as pd
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

app = Flask(__name__)

BASE_DIR = Path(__file__).resolve().parent

# MySQL password sirf ek baar enter hoga
MYSQL_PASSWORD = getpass.getpass(
    "Enter MySQL root password: "
)


def get_connection():
    return mysql.connector.connect(
        host="localhost",
        user="root",
        password=MYSQL_PASSWORD,
        database="medicine_finder"
    )


def load_stock_data():
    connection = get_connection()

    try:
        query = """
            SELECT
                m.name AS medicine,
                m.strength,
                p.name AS pharmacy,
                p.city,
                ps.quantity,
                ps.price
            FROM pharmacy_stock ps
            JOIN medicines m
                ON ps.medicine_id = m.id
            JOIN pharmacies p
                ON ps.pharmacy_id = p.id
            ORDER BY m.name, p.name
        """

        return pd.read_sql(query, connection)

    finally:
        connection.close()


def create_charts(df):
    if df.empty:
        return

    # Graph 1: Medicine-wise stock quantity
    chart_data = (
        df.groupby("medicine")["quantity"]
        .sum()
        .sort_values(ascending=False)
        .head(10)
    )

    plt.figure(figsize=(10, 5))
    chart_data.plot(kind="bar")
    plt.title("Top 10 Medicines by Stock Quantity")
    plt.xlabel("Medicine")
    plt.ylabel("Quantity")
    plt.xticks(rotation=35, ha="right")
    plt.tight_layout()
    plt.savefig(BASE_DIR / "website_stock_chart.png")
    plt.close()

    # Graph 2: In-stock vs out-of-stock records
    available = int((df["quantity"] > 0).sum())
    out_of_stock = int((df["quantity"] == 0).sum())

    plt.figure(figsize=(7, 5))
    plt.bar(
        ["Available", "Out of Stock"],
        [available, out_of_stock]
    )
    plt.title("Medicine Stock Availability")
    plt.ylabel("Number of Stock Records")
    plt.tight_layout()
    plt.savefig(BASE_DIR / "website_stock_status.png")
    plt.close()


PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1">
    <title>Medicine Stock Analytics</title>

    <style>
        * { box-sizing: border-box; }

        body {
            font-family: Arial, sans-serif;
            background: #f1f5f9;
            color: #1e293b;
            margin: 0;
            padding: 20px;
        }

        header {
            background: #0f766e;
            color: white;
            padding: 22px;
            border-radius: 12px;
            margin-bottom: 20px;
        }

        header h1 { margin-top: 0; }

        .cards {
            display: grid;
            grid-template-columns:
                repeat(auto-fit, minmax(160px, 1fr));
            gap: 15px;
        }

        .card, .panel {
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 2px 8px #00000012;
        }

        .number {
            font-size: 28px;
            font-weight: bold;
            margin-top: 10px;
            color: #0f766e;
        }

        .panel {
            margin-top: 20px;
            overflow-x: auto;
        }

        img {
            display: block;
            width: 100%;
            max-width: 900px;
            height: auto;
            margin: auto;
        }

        table {
            border-collapse: collapse;
            width: 100%;
            min-width: 650px;
        }

        th, td {
            text-align: left;
            padding: 11px;
            border-bottom: 1px solid #e2e8f0;
        }

        th {
            background: #0f766e;
            color: white;
        }

        tr:hover { background: #f0fdfa; }

        .error {
            background: #fee2e2;
            color: #991b1b;
            padding: 16px;
            border-radius: 8px;
        }

        .refresh {
            display: inline-block;
            margin-top: 10px;
            padding: 10px 16px;
            background: white;
            color: #0f766e;
            text-decoration: none;
            border-radius: 6px;
            font-weight: bold;
        }
    </style>
</head>

<body>
    <header>
        <h1>Medicine Stock Analytics</h1>
        <p>
            Live inventory report for Medicine Availability Finder.
        </p>
        <a class="refresh" href="/">Refresh Analytics</a>
    </header>

    {% if error %}
        <div class="error">
            <b>Unable to load analytics.</b>
            <p>{{ error }}</p>
            <p>
                Check that MySQL is running, the password is correct,
                and the database tables exist.
            </p>
        </div>
    {% else %}

    <section class="cards">
        <div class="card">
            Total Stock Records
            <div class="number">{{ total_records }}</div>
        </div>

        <div class="card">
            In-stock Records
            <div class="number">{{ available }}</div>
        </div>

        <div class="card">
            Out-of-stock Records
            <div class="number">{{ out_of_stock }}</div>
        </div>

        <div class="card">
            Low-stock Records (1–5)
            <div class="number">{{ low_stock }}</div>
        </div>

        <div class="card">
            Total Medicine Units
            <div class="number">{{ total_quantity }}</div>
        </div>
    </section>

    {% if has_data %}
    <section class="panel">
        <h2>Medicine-wise Stock Quantity</h2>
        <img src="/chart/stock" alt="Medicine stock quantity chart">
    </section>

    <section class="panel">
        <h2>Stock Availability</h2>
        <img src="/chart/status" alt="Stock availability chart">
    </section>
    {% else %}
    <section class="panel">
        No stock records found in the database yet.
    </section>
    {% endif %}

    <section class="panel">
        <h2>Pharmacy Stock Details</h2>

        <table>
            <thead>
                <tr>
                    <th>Medicine</th>
                    <th>Strength</th>
                    <th>Pharmacy</th>
                    <th>City</th>
                    <th>Quantity</th>
                    <th>Price</th>
                </tr>
            </thead>

            <tbody>
                {% for row in rows %}
                <tr>
                    <td>{{ row.medicine }}</td>
                    <td>{{ row.strength or "-" }}</td>
                    <td>{{ row.pharmacy }}</td>
                    <td>{{ row.city or "-" }}</td>
                    <td>{{ row.quantity }}</td>
                    <td>
                        {% if row.price != "" and row.price is not none %}
                            ₹{{ row.price }}
                        {% else %}
                            -
                        {% endif %}
                    </td>
                </tr>
                {% endfor %}
            </tbody>
        </table>
    </section>

    {% endif %}
</body>
</html>
"""


@app.route("/")
def dashboard():
    try:
        df = load_stock_data()

        # Generate chart images from current database data
        create_charts(df)

        total_records = len(df)
        available = int((df["quantity"] > 0).sum())
        out_of_stock = int((df["quantity"] == 0).sum())
        low_stock = int(
            ((df["quantity"] > 0) & (df["quantity"] <= 5)).sum()
        )
        total_quantity = int(df["quantity"].sum())

        rows = df.fillna("").to_dict(orient="records")

        return render_template_string(
            PAGE,
            total_records=total_records,
            available=available,
            out_of_stock=out_of_stock,
            low_stock=low_stock,
            total_quantity=total_quantity,
            rows=rows,
            has_data=not df.empty,
            error=None
        )

    except Exception as e:
        # Error is shown on the dashboard for troubleshooting
        return render_template_string(
            PAGE,
            total_records=0,
            available=0,
            out_of_stock=0,
            low_stock=0,
            total_quantity=0,
            rows=[],
            has_data=False,
            error=str(e)
        ), 500


@app.route("/chart/<name>")
def chart(name):
    chart_files = {
        "stock": BASE_DIR / "website_stock_chart.png",
        "status": BASE_DIR / "website_stock_status.png"
    }

    filename = chart_files.get(name)

    if filename is None or not filename.exists():
        return "Chart not found. Refresh the analytics dashboard.", 404

    return send_file(filename, mimetype="image/png")


if __name__ == "__main__":
    print("Medicine Analytics service starting...")
    print("Open http://127.0.0.1:5000 in your browser.")
    print("Keep this terminal open while viewing analytics.")

    # Local computer access only; do not expose this demo directly
    # to the internet.
    app.run(host="127.0.0.1", port=5000, debug=False)