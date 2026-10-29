import getpass
from pathlib import Path

from flask import Flask, render_template_string, request, send_file
import mysql.connector
import pandas as pd
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

app = Flask(__name__)
BASE_DIR = Path(__file__).resolve().parent
MYSQL_PASSWORD = getpass.getpass("Enter MySQL root password: ")


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
            JOIN medicines m ON ps.medicine_id = m.id
            JOIN pharmacies p ON ps.pharmacy_id = p.id
            ORDER BY m.name, p.name
        """
        return pd.read_sql(query, connection)
    finally:
        connection.close()


# ---------------- DAA ALGORITHMS ----------------
def binary_search_medicine(sorted_names, target):
    """Iterative Binary Search. Input names must be sorted case-insensitively."""
    target_key = target.strip().casefold()
    left, right = 0, len(sorted_names) - 1

    while left <= right:
        mid = (left + right) // 2
        current_key = str(sorted_names[mid]).casefold()

        if current_key == target_key:
            return mid
        if current_key < target_key:
            left = mid + 1
        else:
            right = mid - 1
    return -1


def merge_sort_pharmacies(records):
    """Merge Sort by numeric price ascending; missing prices go last."""
    if len(records) <= 1:
        return records[:]

    middle = len(records) // 2
    left = merge_sort_pharmacies(records[:middle])
    right = merge_sort_pharmacies(records[middle:])

    merged = []
    i = j = 0

    def key(record):
        price = record.get("price")
        try:
            if price is None or pd.isna(price) or str(price).strip() == "":
                return (1, float("inf"))
            return (0, float(price))
        except (TypeError, ValueError):
            return (1, float("inf"))

    while i < len(left) and j < len(right):
        if key(left[i]) <= key(right[j]):
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1

    merged.extend(left[i:])
    merged.extend(right[j:])
    return merged


def greedy_cheapest_available(records):
    """
    Greedy choice: select the lowest-price pharmacy among records
    with quantity > 0 and a valid price.
    """
    eligible = []
    for record in records:
        try:
            quantity = int(record.get("quantity", 0))
            price_value = record.get("price")
            if quantity <= 0 or price_value is None or pd.isna(price_value):
                continue
            price = float(price_value)
            if price < 0:
                continue
            eligible.append((price, record))
        except (TypeError, ValueError):
            continue

    if not eligible:
        return None
    return min(eligible, key=lambda item: item[0])[1]


def create_charts(df):
    if df.empty:
        return

    medicine_stock = (
        df.groupby("medicine")["quantity"]
        .sum()
        .sort_values(ascending=False)
        .head(10)
    )
    plt.figure(figsize=(10, 5))
    medicine_stock.plot(kind="bar")
    plt.title("Top 10 Medicines by Stock Quantity")
    plt.xlabel("Medicine")
    plt.ylabel("Total Units")
    plt.xticks(rotation=35, ha="right")
    plt.tight_layout()
    plt.savefig(BASE_DIR / "website_stock_chart.png")
    plt.close()

    available = int((df["quantity"] > 0).sum())
    out_of_stock = int((df["quantity"] == 0).sum())
    plt.figure(figsize=(7, 5))
    plt.bar(["Available", "Out of Stock"], [available, out_of_stock])
    plt.title("Medicine Stock Availability")
    plt.ylabel("Stock Records")
    plt.tight_layout()
    plt.savefig(BASE_DIR / "website_stock_status.png")
    plt.close()

    city_stock = (
        df.assign(city=df["city"].fillna("").str.strip())
        .groupby("city")["quantity"]
        .sum()
        .sort_values(ascending=False)
    )
    city_stock = city_stock[city_stock.index != ""].head(10)
    if not city_stock.empty:
        plt.figure(figsize=(9, 5))
        city_stock.plot(kind="bar")
        plt.title("Total Medicine Stock by City")
        plt.xlabel("City")
        plt.ylabel("Total Units")
        plt.xticks(rotation=35, ha="right")
        plt.tight_layout()
        plt.savefig(BASE_DIR / "website_city_stock.png")
        plt.close()


PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Medicine Stock Analytics + DAA</title>
<style>
*{box-sizing:border-box}
body{font-family:Arial,sans-serif;background:#f1f5f9;color:#1e293b;margin:0;padding:20px}
header{background:#0f766e;color:white;padding:22px;border-radius:12px;margin-bottom:20px}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:15px}
.card,.panel{background:white;padding:20px;border-radius:12px;box-shadow:0 2px 8px #00000012}
.number{font-size:28px;font-weight:bold;margin-top:10px;color:#0f766e}
.panel{margin-top:20px;overflow-x:auto}
img{display:block;width:100%;max-width:900px;height:auto;margin:auto}
table{border-collapse:collapse;width:100%;min-width:650px}
th,td{text-align:left;padding:11px;border-bottom:1px solid #e2e8f0}
th{background:#0f766e;color:white}
tr:hover{background:#f0fdfa}
.error{background:#fee2e2;color:#991b1b;padding:16px;border-radius:8px}
.refresh{display:inline-block;margin-top:10px;padding:10px 16px;background:white;color:#0f766e;text-decoration:none;border-radius:6px;font-weight:bold}
.muted{color:#64748b}
form{display:flex;flex-wrap:wrap;gap:10px;align-items:end}
label{display:block;font-weight:bold;margin-bottom:6px}
input{padding:10px;border:1px solid #cbd5e1;border-radius:6px;min-width:220px}
button{padding:11px 16px;border:0;border-radius:6px;background:#0f766e;color:white;font-weight:bold;cursor:pointer}
.result{background:#f0fdfa;padding:12px;border-radius:8px;margin-top:12px}
</style>
</head>
<body>
<header>
<h1>Medicine Stock Analytics</h1>
<p>Live reports from the Medicine Finder database.</p>
<a class="refresh" href="/">Refresh Analytics</a>
</header>

{% if error %}
<div class="error"><b>Unable to load analytics.</b><p>{{ error }}</p><p>Check MySQL and the database connection.</p></div>
{% else %}
<section class="cards">
<div class="card">Total Stock Records<div class="number">{{ total_records }}</div></div>
<div class="card">In-stock Records<div class="number">{{ available }}</div></div>
<div class="card">Out-of-stock Records<div class="number">{{ out_of_stock }}</div></div>
<div class="card">Low-stock Records (1–5)<div class="number">{{ low_stock }}</div></div>
<div class="card">Total Medicine Units<div class="number">{{ total_quantity }}</div></div>
</section>

<section class="panel"><h2>Top Medicines by Stock</h2>
{% if has_data %}<img src="/chart/stock" alt="Medicine stock chart">{% else %}<p class="muted">No stock records available yet.</p>{% endif %}</section>
<section class="panel"><h2>Stock Availability</h2>
{% if has_data %}<img src="/chart/status" alt="Stock status chart">{% else %}<p class="muted">No stock records available yet.</p>{% endif %}</section>

<section class="panel">
<h2>DAA Algorithms — Live Medicine Data</h2>
<p>Binary Search finds a medicine in a sorted list; Merge Sort orders its pharmacy offers by price; Greedy selects the cheapest in-stock offer with a valid price.</p>
<form method="get" action="/">
<div><label for="medicine">Medicine name</label><input id="medicine" name="medicine" value="{{ search_term }}" placeholder="e.g. Paracetamol"></div>
<button type="submit">Run DAA Search</button>
</form>
{% if search_term %}
<div class="result">
<h3>Binary Search Result</h3>
{% if found_medicine %}<p>Medicine found: <b>{{ found_medicine }}</b></p>{% else %}<p><b>{{ search_term }}</b> was not found in the database medicine list.</p>{% endif %}
<p class="muted">Algorithm: Binary Search | Time complexity: O(log n) on sorted unique medicine names.</p>
</div>
{% if offers %}
<h3>Pharmacy offers sorted by Merge Sort (price ascending)</h3>
<table><thead><tr><th>Medicine</th><th>Pharmacy</th><th>City</th><th>Quantity</th><th>Price</th></tr></thead><tbody>
{% for row in offers %}<tr><td>{{ row.medicine }}</td><td>{{ row.pharmacy }}</td><td>{{ row.city or "-" }}</td><td>{{ row.quantity }}</td><td>{% if row.price is not none and row.price != "" %}₹{{ row.price }}{% else %}-{% endif %}</td></tr>{% endfor %}
</tbody></table>
<p class="muted">Merge Sort | Time complexity: O(n log n).</p>
{% elif found_medicine %}<p>No pharmacy stock records found for this medicine.</p>{% endif %}
{% if recommendation %}
<div class="result"><h3>Greedy Recommendation: Cheapest In-stock Pharmacy</h3>
<p><b>{{ recommendation.pharmacy }}</b> — {{ recommendation.city or "City not specified" }}</p>
<p>Medicine: {{ recommendation.medicine }} | Quantity: {{ recommendation.quantity }} | Price: ₹{{ recommendation.price }}</p>
<p class="muted">Greedy selection examines eligible offers and chooses the minimum price (O(n)). This is a price-only recommendation, not a medical recommendation.</p>
</div>
{% elif found_medicine %}
<p>No in-stock pharmacy with a valid price is available for this medicine.</p>
{% endif %}
{% endif %}
</section>

<section class="panel"><h2>Top 10 Medicines Ranking</h2>
{% if ranking_rows %}<table><thead><tr><th>Rank</th><th>Medicine</th><th>Total Stock Units</th></tr></thead><tbody>
{% for item in ranking_rows %}<tr><td>{{ item.rank }}</td><td>{{ item.medicine }}</td><td>{{ item.total_units }}</td></tr>{% endfor %}
</tbody></table>{% else %}<p>No medicine ranking data available yet.</p>{% endif %}
</section>

<section class="panel"><h2>Low Stock Alerts</h2><p>Medicines with 1–5 units at a pharmacy.</p>
{% if low_stock_rows %}<table><thead><tr><th>Medicine</th><th>Pharmacy</th><th>City</th><th>Quantity</th><th>Status</th></tr></thead><tbody>
{% for item in low_stock_rows %}<tr><td>{{ item.medicine }}</td><td>{{ item.pharmacy }}</td><td>{{ item.city or "-" }}</td><td>{{ item.quantity }}</td><td>Low Stock</td></tr>{% endfor %}
</tbody></table>{% else %}<p>There are no medicines with stock between 1 and 5 units.</p>{% endif %}
</section>

<section class="panel"><h2>Out of Stock Alerts</h2><p>Medicines with zero units at a pharmacy.</p>
{% if out_of_stock_rows %}<table><thead><tr><th>Medicine</th><th>Pharmacy</th><th>City</th><th>Quantity</th><th>Status</th></tr></thead><tbody>
{% for item in out_of_stock_rows %}<tr><td>{{ item.medicine }}</td><td>{{ item.pharmacy }}</td><td>{{ item.city or "-" }}</td><td>{{ item.quantity }}</td><td>Out of Stock</td></tr>{% endfor %}
</tbody></table>{% else %}<p>There are no medicines with zero stock.</p>{% endif %}
</section>

<section class="panel"><h2>City-wise Medicine Stock</h2>
{% if city_rows %}<img src="/chart/city" alt="City-wise stock chart"><table><thead><tr><th>City</th><th>Total Stock Units</th><th>Stock Records</th></tr></thead><tbody>
{% for row in city_rows %}<tr><td>{{ row.city }}</td><td>{{ row.total_units }}</td><td>{{ row.records }}</td></tr>{% endfor %}
</tbody></table>{% else %}<p>No city information is available in the stock records.</p>{% endif %}
</section>

<section class="panel"><h2>Pharmacy Stock Details</h2><table><thead><tr><th>Medicine</th><th>Strength</th><th>Pharmacy</th><th>City</th><th>Quantity</th><th>Price</th></tr></thead><tbody>
{% for row in rows %}<tr><td>{{ row.medicine }}</td><td>{{ row.strength or "-" }}</td><td>{{ row.pharmacy }}</td><td>{{ row.city or "-" }}</td><td>{{ row.quantity }}</td><td>{% if row.price is not none and row.price != "" %}₹{{ row.price }}{% else %}-{% endif %}</td></tr>{% endfor %}
</tbody></table></section>
{% endif %}
</body></html>
"""


@app.route("/")
def dashboard():
    try:
        df = load_stock_data()
        create_charts(df)

        # Ensure consistent numeric handling for algorithm and reports
        df["quantity"] = pd.to_numeric(df["quantity"], errors="coerce").fillna(0).astype(int)
        df["price"] = pd.to_numeric(df["price"], errors="coerce")

        total_records = len(df)
        available = int((df["quantity"] > 0).sum())
        out_of_stock = int((df["quantity"] == 0).sum())
        low_stock = int(((df["quantity"] >= 1) & (df["quantity"] <= 5)).sum())
        total_quantity = int(df["quantity"].sum())

        ranking_data = (
            df.groupby("medicine")["quantity"].sum()
            .sort_values(ascending=False).reset_index()
        )
        ranking_data.columns = ["medicine", "total_units"]
        ranking_data.insert(0, "rank", range(1, len(ranking_data) + 1))
        ranking_rows = ranking_data.head(10).to_dict(orient="records")

        low_stock_rows = df[(df["quantity"] >= 1) & (df["quantity"] <= 5)].copy().to_dict(orient="records")
        out_of_stock_rows = df[df["quantity"] == 0].copy().to_dict(orient="records")

        city_data = df.copy()
        city_data["city"] = city_data["city"].fillna("").str.strip()
        city_data = city_data[city_data["city"] != ""]
        if city_data.empty:
            city_rows = []
        else:
            summary = (
                city_data.groupby("city")
                .agg(total_units=("quantity", "sum"), records=("quantity", "size"))
                .sort_values("total_units", ascending=False)
                .reset_index().head(10)
            )
            city_rows = summary.to_dict(orient="records")

        # Build a case-insensitive sorted unique medicine list for Binary Search.
        medicine_names = sorted(
            df["medicine"].dropna().astype(str).unique().tolist(),
            key=str.casefold
        )
        search_term = request.args.get("medicine", "").strip()
        found_medicine = None
        offers = []
        recommendation = None

        if search_term:
            found_index = binary_search_medicine(medicine_names, search_term)
            if found_index >= 0:
                found_medicine = medicine_names[found_index]
                matched_records = df[
                    df["medicine"].astype(str).str.casefold() == found_medicine.casefold()
                ].copy()
                offers_records = matched_records.to_dict(orient="records")
                offers = merge_sort_pharmacies(offers_records)
                recommendation = greedy_cheapest_available(offers_records)

        rows = df.fillna("").to_dict(orient="records")
        return render_template_string(
            PAGE,
            total_records=total_records,
            available=available,
            out_of_stock=out_of_stock,
            low_stock=low_stock,
            total_quantity=total_quantity,
            rows=rows,
            ranking_rows=ranking_rows,
            low_stock_rows=low_stock_rows,
            out_of_stock_rows=out_of_stock_rows,
            city_rows=city_rows,
            has_data=not df.empty,
            search_term=search_term,
            found_medicine=found_medicine,
            offers=offers,
            recommendation=recommendation,
            error=None
        )
    except Exception as e:
        app.logger.exception("Could not load analytics")
        return render_template_string(
            PAGE,
            total_records=0, available=0, out_of_stock=0, low_stock=0,
            total_quantity=0, rows=[], ranking_rows=[], low_stock_rows=[],
            out_of_stock_rows=[], city_rows=[], has_data=False,
            search_term=request.args.get("medicine", "").strip(),
            found_medicine=None, offers=[], recommendation=None, error=str(e)
        ), 500


@app.route("/chart/<name>")
def chart(name):
    chart_files = {
        "stock": BASE_DIR / "website_stock_chart.png",
        "status": BASE_DIR / "website_stock_status.png",
        "city": BASE_DIR / "website_city_stock.png"
    }
    filename = chart_files.get(name)
    if filename is None or not filename.exists():
        return "Chart not found. Refresh the dashboard.", 404
    return send_file(filename, mimetype="image/png")


# Public for quick manual testing if running this file directly.
if __name__ == "__main__":
    print("Medicine Analytics + DAA service starting...")
    print("Open http://127.0.0.1:5000 in your browser.")
    print("Keep this terminal open while viewing analytics.")
    app.run(host="127.0.0.1", port=5000, debug=False)
