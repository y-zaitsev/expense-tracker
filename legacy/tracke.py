import json
from datetime import datetime

FILE_NAME = "expenses.json"


# File Operations 

def load_expenses():
    """Load expenses from a JSON file."""
    try:
        with open(FILE_NAME, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def save_expenses(expenses):
    """Save the list of expenses to a JSON file."""
    with open(FILE_NAME, "w", encoding="utf-8") as f:
        json.dump(expenses, f, indent=2, ensure_ascii=False)


#  Input Validation 

def get_amount():
    """Ask for an amount until the user enters a positive number."""
    while True:
        text = input("Amount (€): ").replace(",", ".")

        try:
            amount = float(text)
        except ValueError:
            print("Error: please enter a number, e.g., 12.50")
            continue

        if amount <= 0:
            print("Error: amount must be a positive number.")
            continue

        return round(amount, 2)


def get_date():
    """Ask for a date in YYYY-MM-DD format. Use today's date if empty."""
    while True:
        text = input(
            "Date (YYYY-MM-DD, Enter = Today): "
        ).strip()

        if text == "":
            return datetime.now().strftime("%Y-%m-%d")

        try:
            datetime.strptime(text, "%Y-%m-%d")
            return text
        except ValueError:
            print(
                "Error: date format must be YYYY-MM-DD, "
                "for example 2026-10-26"
            )


def get_category():
    """Ask for a non-empty expense category."""
    while True:
        category = input(
            "Category (e.g., Food): "
        ).strip().capitalize()

        if category:
            return category

        print("Error: category cannot be empty.")


# Main Operations 

def add_expense(expenses):
    """Add a new expense and save it to the file."""
    amount = get_amount()
    category = get_category()
    date = get_date()

    expenses.append({
        "amount": amount,
        "category": category,
        "date": date
    })

    save_expenses(expenses)
    print("Expense saved!")


def view_expenses(expenses):
    """Display all recorded expenses."""
    if not expenses:
        print("No expenses recorded yet.")
        return

    print(f"\n{'#':<4}{'Date':<12}{'Category':<15}{'Amount':>10}")
    print("-" * 41)

    for i, expense in enumerate(expenses, start=1):
        print(
            f"{i:<4}{expense['date']:<12}"
            f"{expense['category']:<15}"
            f"{expense['amount']:>9.2f}€"
        )


def summarize_by_category(expenses):
    """Calculate and display expense totals by category."""
    if not expenses:
        print("No expenses recorded yet.")
        return

    totals = {}

    for expense in expenses:
        category = expense["category"]
        totals[category] = (
            totals.get(category, 0) + expense["amount"]
        )

    print("\nSummaries by category:")

    for category, total in sorted(totals.items()):
        print(f"  {category}: €{total:.2f}")

    print(f"  TOTAL: €{sum(totals.values()):.2f}")


#  Main Menu 

def main():
    """Run the expense tracker application."""
    expenses = load_expenses()

    while True:
        print("\n=== Expense Tracker ===")
        print("1. Add Expense")
        print("2. View All Expenses")
        print("3. Summarize by Category")
        print("4. Exit")

        choice = input("Choose an option: ").strip()

        if choice == "1":
            add_expense(expenses)
        elif choice == "2":
            view_expenses(expenses)
        elif choice == "3":
            summarize_by_category(expenses)
        elif choice == "4":
            print("Goodbye!")
            break
        else:
            print("Invalid option, please enter 1, 2, 3, or 4.")


if __name__ == "__main__":
    main()
