# Finance Controller Database — Schema Reference

PostgreSQL 17 • Schema: `public` • pgvector enabled

## business_units
Organizational divisions with regional assignment and staffing.

| Column | Type | Notes |
|--------|------|-------|
| id | serial PK | |
| name | varchar(50) | NOT NULL; e.g. Engineering, Sales |
| region | varchar(30) | NOT NULL; geographic region |
| cost_center | varchar(10) | NOT NULL, UNIQUE; e.g. CC-1001 |
| head_count | int | Current employee count |
| created_at | timestamptz | Default now() |

## chart_of_accounts
General ledger account master. Hierarchical via parent_account_id.

| Column | Type | Notes |
|--------|------|-------|
| account_id | varchar(10) PK | GL code; e.g. 5000, 4100 |
| account_name | varchar(100) | NOT NULL; descriptive name |
| account_type | varchar(10) | NOT NULL; ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE |
| parent_account_id | varchar(10) FK → chart_of_accounts | NULL for top-level |
| is_active | boolean | Default true |

## journal_entries
Header for each accounting transaction. One entry has many journal_lines.

| Column | Type | Notes |
|--------|------|-------|
| entry_id | serial PK | |
| entry_date | date | NOT NULL; transaction date |
| description | varchar(200) | NOT NULL; what this entry records |
| created_by | varchar(50) | NOT NULL; user or system |
| approved_by | varchar(50) | NULL if pending approval |
| status | varchar(10) | POSTED, PENDING, or REVERSED |

## journal_lines
Double-entry line items. Each line debits or credits one account for one BU.

| Column | Type | Notes |
|--------|------|-------|
| line_id | serial PK | |
| entry_id | int FK → journal_entries | NOT NULL |
| account_id | varchar(10) FK → chart_of_accounts | NOT NULL |
| business_unit_id | int FK → business_units | NOT NULL |
| debit_amount | numeric(12,2) | Default 0 |
| credit_amount | numeric(12,2) | Default 0 |
| currency | varchar(3) | Default 'USD' |
| description | varchar(200) | Line-level detail |

**Key:** For expense analysis, sum debit_amount on EXPENSE accounts. For revenue, sum credit_amount on REVENUE accounts.

## budgets
Annual budget allocations broken into quarters. One row per BU × account × year.

| Column | Type | Notes |
|--------|------|-------|
| budget_id | serial PK | |
| fiscal_year | int | NOT NULL; e.g. 2024 |
| business_unit_id | int FK → business_units | NOT NULL |
| account_id | varchar(10) FK → chart_of_accounts | NOT NULL |
| q1_amount | numeric(12,2) | Jan-Mar budget |
| q2_amount | numeric(12,2) | Apr-Jun budget |
| q3_amount | numeric(12,2) | Jul-Sep budget |
| q4_amount | numeric(12,2) | Oct-Dec budget |
| annual_total | numeric(12,2) | Generated: sum of q1-q4 |

## invoices
Vendor invoices with payment lifecycle tracking.

| Column | Type | Notes |
|--------|------|-------|
| invoice_id | serial PK | |
| vendor_name | varchar(100) | NOT NULL |
| invoice_date | date | NOT NULL; when issued |
| due_date | date | NOT NULL; payment deadline |
| amount | numeric(12,2) | NOT NULL; invoice total |
| currency | varchar(3) | Default 'USD' |
| status | varchar(12) | PAID, OUTSTANDING, OVERDUE, DISPUTED |
| business_unit_id | int FK → business_units | NOT NULL |
| account_id | varchar(10) FK → chart_of_accounts | NOT NULL; expense account |
| paid_date | date | NULL unless status = PAID |

## cash_flow
Cash movements categorized by activity type.

| Column | Type | Notes |
|--------|------|-------|
| flow_id | serial PK | |
| flow_date | date | NOT NULL |
| flow_type | varchar(10) | OPERATING, INVESTING, FINANCING |
| description | varchar(200) | NOT NULL |
| amount | numeric(12,2) | NOT NULL; positive=inflow, negative=outflow |
| business_unit_id | int FK → business_units | NOT NULL |

# Known Categorical Values

**account_type (chart_of_accounts):** ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE

**status (journal_entries):** POSTED, PENDING, REVERSED

**status (invoices):** PAID, OUTSTANDING, OVERDUE, DISPUTED

**flow_type (cash_flow):** OPERATING, INVESTING, FINANCING

**region (business_units):** North America, EMEA, APAC

**Business Unit names:** Engineering, Sales, Marketing, Operations, Finance, HR

**Account numbering convention:**
- 1000-1999: Assets
- 2000-2999: Liabilities
- 3000-3999: Equity
- 4000-4999: Revenue
- 5000-6999: Expenses

# Key Relationships

- journal_lines → journal_entries (entry_id) — lines belong to one entry
- journal_lines → chart_of_accounts (account_id) — which GL account
- journal_lines → business_units (business_unit_id) — cost allocation
- budgets → business_units + chart_of_accounts — budget per BU per account
- invoices → business_units + chart_of_accounts — vendor spend by BU
- cash_flow → business_units — cash movements by BU

# Example Queries

<example>
<question>What is total revenue YTD by business unit?</question>
<thought_process>Revenue = credit_amount on REVENUE accounts (4xxx). Join journal_lines to chart_of_accounts filtering account_type='REVENUE', then group by business unit.</thought_process>
<sql>
SELECT bu.name AS business_unit, SUM(jl.credit_amount) AS total_revenue
FROM journal_lines jl
JOIN journal_entries je ON je.entry_id = jl.entry_id
JOIN chart_of_accounts coa ON coa.account_id = jl.account_id
JOIN business_units bu ON bu.id = jl.business_unit_id
WHERE coa.account_type = 'REVENUE' AND je.status = 'POSTED'
  AND je.entry_date >= '2024-01-01'
GROUP BY bu.name
ORDER BY total_revenue DESC;
</sql>
</example>

<example>
<question>Which business units are over budget for Q3?</question>
<thought_process>Compare actual Q3 expense (debit_amount where entry_date Jul-Sep on EXPENSE accounts) vs budget q3_amount, grouped by BU + account. Filter where actual > budget.</thought_process>
<sql>
SELECT bu.name, coa.account_name,
       SUM(jl.debit_amount) AS actual_q3,
       b.q3_amount AS budget_q3,
       SUM(jl.debit_amount) - b.q3_amount AS variance
FROM journal_lines jl
JOIN journal_entries je ON je.entry_id = jl.entry_id
JOIN chart_of_accounts coa ON coa.account_id = jl.account_id
JOIN business_units bu ON bu.id = jl.business_unit_id
JOIN budgets b ON b.business_unit_id = bu.id AND b.account_id = jl.account_id AND b.fiscal_year = 2024
WHERE coa.account_type = 'EXPENSE' AND je.status = 'POSTED'
  AND je.entry_date >= '2024-07-01' AND je.entry_date < '2024-10-01'
GROUP BY bu.name, coa.account_name, b.q3_amount
HAVING SUM(jl.debit_amount) > b.q3_amount
ORDER BY variance DESC;
</sql>
</example>

<example>
<question>Show me all overdue invoices over $10,000</question>
<thought_process>Filter invoices where status='OVERDUE' and amount > 10000. Include vendor, BU, and days overdue.</thought_process>
<sql>
SELECT i.vendor_name, i.amount, i.due_date,
       CURRENT_DATE - i.due_date AS days_overdue,
       bu.name AS business_unit
FROM invoices i
JOIN business_units bu ON bu.id = i.business_unit_id
WHERE i.status = 'OVERDUE' AND i.amount > 10000
ORDER BY i.amount DESC;
</sql>
</example>

<example>
<question>What is our operating cash flow trend by month?</question>
<thought_process>Filter cash_flow where flow_type='OPERATING', truncate flow_date to month, sum amounts.</thought_process>
<sql>
SELECT DATE_TRUNC('month', flow_date) AS month,
       SUM(amount) AS net_operating_cash_flow
FROM cash_flow
WHERE flow_type = 'OPERATING'
GROUP BY month
ORDER BY month;
</sql>
</example>

<example>
<question>What are the top expense accounts by total spend?</question>
<thought_process>Sum debit_amount on EXPENSE accounts from posted journal lines, group by account.</thought_process>
<sql>
SELECT coa.account_id, coa.account_name, SUM(jl.debit_amount) AS total_spend
FROM journal_lines jl
JOIN journal_entries je ON je.entry_id = jl.entry_id
JOIN chart_of_accounts coa ON coa.account_id = jl.account_id
WHERE coa.account_type = 'EXPENSE' AND je.status = 'POSTED'
GROUP BY coa.account_id, coa.account_name
ORDER BY total_spend DESC
LIMIT 10;
</sql>
</example>

<example>
<question>Show quarter-over-quarter revenue growth</question>
<thought_process>Sum revenue (credit_amount on REVENUE accounts) per quarter, compute growth vs prior quarter using LAG.</thought_process>
<sql>
WITH quarterly AS (
    SELECT EXTRACT(QUARTER FROM je.entry_date) AS quarter,
           SUM(jl.credit_amount) AS revenue
    FROM journal_lines jl
    JOIN journal_entries je ON je.entry_id = jl.entry_id
    JOIN chart_of_accounts coa ON coa.account_id = jl.account_id
    WHERE coa.account_type = 'REVENUE' AND je.status = 'POSTED'
      AND je.entry_date >= '2024-01-01' AND je.entry_date < '2025-01-01'
    GROUP BY quarter
)
SELECT quarter, revenue,
       LAG(revenue) OVER (ORDER BY quarter) AS prev_quarter,
       ROUND((revenue - LAG(revenue) OVER (ORDER BY quarter)) / LAG(revenue) OVER (ORDER BY quarter) * 100, 1) AS growth_pct
FROM quarterly
ORDER BY quarter;
</sql>
</example>

<example>
<question>Compare total expenses across business units</question>
<thought_process>Sum debit_amount on EXPENSE accounts grouped by BU name for posted entries in FY2024.</thought_process>
<sql>
SELECT bu.name AS business_unit, bu.region,
       SUM(jl.debit_amount) AS total_expenses,
       bu.head_count,
       ROUND(SUM(jl.debit_amount) / bu.head_count, 2) AS cost_per_head
FROM journal_lines jl
JOIN journal_entries je ON je.entry_id = jl.entry_id
JOIN chart_of_accounts coa ON coa.account_id = jl.account_id
JOIN business_units bu ON bu.id = jl.business_unit_id
WHERE coa.account_type = 'EXPENSE' AND je.status = 'POSTED'
  AND je.entry_date >= '2024-01-01'
GROUP BY bu.name, bu.region, bu.head_count
ORDER BY total_expenses DESC;
</sql>
</example>

<example>
<question>Show journal entries pending approval</question>
<thought_process>Filter journal_entries where status='PENDING', join to lines for amounts.</thought_process>
<sql>
SELECT je.entry_id, je.entry_date, je.description, je.created_by,
       SUM(jl.debit_amount) AS total_amount
FROM journal_entries je
JOIN journal_lines jl ON jl.entry_id = je.entry_id
WHERE je.status = 'PENDING'
GROUP BY je.entry_id, je.entry_date, je.description, je.created_by
ORDER BY je.entry_date DESC;
</sql>
</example>

<example>
<question>Which vendors have the highest total spend?</question>
<thought_process>Group invoices by vendor_name, sum amount. Include count for frequency.</thought_process>
<sql>
SELECT vendor_name,
       COUNT(*) AS invoice_count,
       SUM(amount) AS total_spend,
       SUM(CASE WHEN status = 'OVERDUE' THEN amount ELSE 0 END) AS overdue_amount
FROM invoices
GROUP BY vendor_name
ORDER BY total_spend DESC
LIMIT 10;
</sql>
</example>

<example>
<question>What is the cost per headcount by business unit?</question>
<thought_process>Sum salary expense (account 5000) per BU, divide by head_count from business_units.</thought_process>
<sql>
SELECT bu.name, bu.head_count,
       SUM(jl.debit_amount) AS total_salary_cost,
       ROUND(SUM(jl.debit_amount) / bu.head_count, 2) AS cost_per_employee
FROM journal_lines jl
JOIN journal_entries je ON je.entry_id = jl.entry_id
JOIN business_units bu ON bu.id = jl.business_unit_id
WHERE jl.account_id = '5000' AND je.status = 'POSTED'
GROUP BY bu.name, bu.head_count
ORDER BY cost_per_employee DESC;
</sql>
</example>
