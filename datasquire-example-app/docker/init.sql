-- DataSquire Example App: Finance Controller Dashboard — schema + sample data
-- Auto-executed by Postgres on first container start

-- =============================================================================
-- Extensions
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================================================
-- Schema RAG: schema_chunks table (used by DataSquire for vector search)
-- =============================================================================
CREATE TABLE schema_chunks (
    chunk_id   TEXT NOT NULL,
    namespace  TEXT NOT NULL,
    kind       TEXT,
    title      TEXT,
    content    TEXT,
    embed_text TEXT,
    embedding  vector(1536),
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (chunk_id, namespace)
);

CREATE INDEX idx_schema_chunks_embedding
    ON schema_chunks USING hnsw (embedding vector_cosine_ops);

-- =============================================================================
-- Finance Controller Schema
-- =============================================================================

CREATE TABLE business_units (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    region      VARCHAR(30) NOT NULL,
    cost_center VARCHAR(10) NOT NULL UNIQUE,
    head_count  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE chart_of_accounts (
    account_id       VARCHAR(10) PRIMARY KEY,
    account_name     VARCHAR(100) NOT NULL,
    account_type     VARCHAR(10) NOT NULL CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    parent_account_id VARCHAR(10) REFERENCES chart_of_accounts(account_id),
    is_active        BOOLEAN DEFAULT true
);

CREATE TABLE journal_entries (
    entry_id    SERIAL PRIMARY KEY,
    entry_date  DATE NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_by  VARCHAR(50) NOT NULL,
    approved_by VARCHAR(50),
    status      VARCHAR(10) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED','PENDING','REVERSED'))
);

CREATE TABLE journal_lines (
    line_id          SERIAL PRIMARY KEY,
    entry_id         INT NOT NULL REFERENCES journal_entries(entry_id),
    account_id       VARCHAR(10) NOT NULL REFERENCES chart_of_accounts(account_id),
    business_unit_id INT NOT NULL REFERENCES business_units(id),
    debit_amount     NUMERIC(12,2) DEFAULT 0,
    credit_amount    NUMERIC(12,2) DEFAULT 0,
    currency         VARCHAR(3) DEFAULT 'USD',
    description      VARCHAR(200)
);

CREATE TABLE budgets (
    budget_id        SERIAL PRIMARY KEY,
    fiscal_year      INT NOT NULL,
    business_unit_id INT NOT NULL REFERENCES business_units(id),
    account_id       VARCHAR(10) NOT NULL REFERENCES chart_of_accounts(account_id),
    q1_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    q2_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    q3_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    q4_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    annual_total     NUMERIC(12,2) GENERATED ALWAYS AS (q1_amount + q2_amount + q3_amount + q4_amount) STORED
);

CREATE TABLE invoices (
    invoice_id       SERIAL PRIMARY KEY,
    vendor_name      VARCHAR(100) NOT NULL,
    invoice_date     DATE NOT NULL,
    due_date         DATE NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(3) DEFAULT 'USD',
    status           VARCHAR(12) NOT NULL DEFAULT 'OUTSTANDING' CHECK (status IN ('PAID','OUTSTANDING','OVERDUE','DISPUTED')),
    business_unit_id INT NOT NULL REFERENCES business_units(id),
    account_id       VARCHAR(10) NOT NULL REFERENCES chart_of_accounts(account_id),
    paid_date        DATE
);

CREATE TABLE cash_flow (
    flow_id          SERIAL PRIMARY KEY,
    flow_date        DATE NOT NULL,
    flow_type        VARCHAR(10) NOT NULL CHECK (flow_type IN ('OPERATING','INVESTING','FINANCING')),
    description      VARCHAR(200) NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    business_unit_id INT NOT NULL REFERENCES business_units(id)
);

-- Indexes
CREATE INDEX idx_journal_entries_date ON journal_entries(entry_date);
CREATE INDEX idx_journal_entries_status ON journal_entries(status);
CREATE INDEX idx_journal_lines_entry ON journal_lines(entry_id);
CREATE INDEX idx_journal_lines_account ON journal_lines(account_id);
CREATE INDEX idx_journal_lines_bu ON journal_lines(business_unit_id);
CREATE INDEX idx_budgets_year_bu ON budgets(fiscal_year, business_unit_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_cash_flow_date ON cash_flow(flow_date);
CREATE INDEX idx_cash_flow_type ON cash_flow(flow_type);

-- =============================================================================
-- Seed Data
-- =============================================================================

-- Business Units (6 across 3 regions)
INSERT INTO business_units (name, region, cost_center, head_count) VALUES
('Engineering',  'North America', 'CC-1001', 85),
('Sales',        'North America', 'CC-2001', 45),
('Marketing',    'EMEA',          'CC-3001', 30),
('Operations',   'APAC',          'CC-4001', 60),
('Finance',      'North America', 'CC-5001', 20),
('HR',           'EMEA',          'CC-6001', 15);

-- Chart of Accounts (30 accounts — standard GL structure)
INSERT INTO chart_of_accounts (account_id, account_name, account_type, parent_account_id) VALUES
-- Assets
('1000', 'Cash and Cash Equivalents',   'ASSET', NULL),
('1100', 'Accounts Receivable',          'ASSET', NULL),
('1200', 'Prepaid Expenses',             'ASSET', NULL),
('1300', 'Fixed Assets',                 'ASSET', NULL),
('1400', 'Accumulated Depreciation',     'ASSET', '1300'),
-- Liabilities
('2000', 'Accounts Payable',             'LIABILITY', NULL),
('2100', 'Accrued Liabilities',          'LIABILITY', NULL),
('2200', 'Deferred Revenue',             'LIABILITY', NULL),
('2300', 'Short-term Debt',              'LIABILITY', NULL),
('2400', 'Long-term Debt',               'LIABILITY', NULL),
-- Equity
('3000', 'Common Stock',                 'EQUITY', NULL),
('3100', 'Retained Earnings',            'EQUITY', NULL),
('3200', 'Additional Paid-in Capital',   'EQUITY', NULL),
-- Revenue
('4000', 'Product Revenue',              'REVENUE', NULL),
('4100', 'Service Revenue',              'REVENUE', NULL),
('4200', 'Subscription Revenue',         'REVENUE', NULL),
('4300', 'Consulting Revenue',           'REVENUE', NULL),
('4400', 'Interest Income',              'REVENUE', NULL),
-- Expenses
('5000', 'Salaries and Wages',           'EXPENSE', NULL),
('5100', 'Employee Benefits',            'EXPENSE', NULL),
('5200', 'Cloud Infrastructure',         'EXPENSE', NULL),
('5300', 'Software Licenses',            'EXPENSE', NULL),
('5400', 'Marketing and Advertising',    'EXPENSE', NULL),
('5500', 'Travel and Entertainment',     'EXPENSE', NULL),
('5600', 'Professional Services',        'EXPENSE', NULL),
('5700', 'Rent and Facilities',          'EXPENSE', NULL),
('5800', 'Depreciation',                 'EXPENSE', NULL),
('5900', 'Office Supplies',              'EXPENSE', NULL),
('6000', 'Insurance',                    'EXPENSE', NULL),
('6100', 'Utilities',                    'EXPENSE', NULL);

-- Journal Entries + Lines (200 entries, ~400 lines)
-- Generated via DO block for compactness; tells a story:
--   Engineering overspent Q3 (cloud + contractor surge)
--   Sales has strong revenue but slow collections
--   Marketing underspent budget (campaigns delayed)
DO $$
DECLARE
    v_entry_id INT;
    v_date DATE;
    v_month INT;
    v_bu INT;
    v_amount NUMERIC(12,2);
    i INT;
BEGIN
    -- Monthly salary entries: 12 months × 6 BUs = 72 entries
    FOR v_month IN 1..12 LOOP
        v_date := make_date(2024, v_month, 28);
        FOR v_bu IN 1..6 LOOP
            -- Base salary varies by BU headcount
            v_amount := CASE v_bu
                WHEN 1 THEN 1020000  -- Engineering: 85 × $12K avg
                WHEN 2 THEN 495000   -- Sales: 45 × $11K avg
                WHEN 3 THEN 300000   -- Marketing: 30 × $10K avg
                WHEN 4 THEN 540000   -- Operations: 60 × $9K avg
                WHEN 5 THEN 220000   -- Finance: 20 × $11K avg
                WHEN 6 THEN 135000   -- HR: 15 × $9K avg
            END;
            INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
            VALUES (v_date, 'Monthly payroll - ' || to_char(v_date, 'Mon YYYY'), 'payroll_system', 'cfo_williams', 'POSTED')
            RETURNING entry_id INTO v_entry_id;
            INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
            VALUES
                (v_entry_id, '5000', v_bu, v_amount, 0, 'Salaries'),
                (v_entry_id, '1000', v_bu, 0, v_amount, 'Cash disbursement');
        END LOOP;
    END LOOP;

    -- Engineering cloud infrastructure surge Q3 (overspend story): 20 entries
    FOR i IN 1..20 LOOP
        v_date := make_date(2024, 7 + (i % 3), 5 + i);
        v_amount := 45000 + (random() * 30000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Cloud infrastructure - scaling event #' || i, 'eng_ops', 'vp_eng_chen', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES
            (v_entry_id, '5200', 1, v_amount, 0, 'AWS/GCP compute surge'),
            (v_entry_id, '2000', 1, 0, v_amount, 'Vendor payable');
    END LOOP;

    -- Engineering contractor fees Q3: 10 entries
    FOR i IN 1..10 LOOP
        v_date := make_date(2024, 7 + (i % 3), 10 + i);
        v_amount := 25000 + (random() * 15000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Contractor services - platform rebuild', 'eng_ops', 'vp_eng_chen', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES
            (v_entry_id, '5600', 1, v_amount, 0, 'External contractors'),
            (v_entry_id, '2000', 1, 0, v_amount, 'Vendor payable');
    END LOOP;

    -- Revenue entries (monthly): 48 entries (4 revenue streams × 12 months)
    FOR v_month IN 1..12 LOOP
        v_date := make_date(2024, v_month, 15);
        -- Product Revenue (Sales BU)
        v_amount := 1800000 + (random() * 400000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Product revenue recognition - ' || to_char(v_date, 'Mon YYYY'), 'revenue_system', 'controller_park', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES (v_entry_id, '1100', 2, v_amount, 0, 'AR increase'), (v_entry_id, '4000', 2, 0, v_amount, 'Product sales');

        -- Service Revenue (Operations BU)
        v_amount := 650000 + (random() * 150000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Service revenue - ' || to_char(v_date, 'Mon YYYY'), 'revenue_system', 'controller_park', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES (v_entry_id, '1100', 4, v_amount, 0, 'AR increase'), (v_entry_id, '4100', 4, 0, v_amount, 'Service delivery');

        -- Subscription Revenue (Engineering BU)
        v_amount := 920000 + (random() * 180000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Subscription revenue - ' || to_char(v_date, 'Mon YYYY'), 'revenue_system', 'controller_park', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES (v_entry_id, '1100', 1, v_amount, 0, 'AR increase'), (v_entry_id, '4200', 1, 0, v_amount, 'SaaS subscriptions');

        -- Consulting Revenue (Sales BU)
        v_amount := 280000 + (random() * 70000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Consulting revenue - ' || to_char(v_date, 'Mon YYYY'), 'revenue_system', 'controller_park', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES (v_entry_id, '1100', 2, v_amount, 0, 'AR increase'), (v_entry_id, '4300', 2, 0, v_amount, 'Consulting engagements');
    END LOOP;

    -- Marketing expenses (underspent — campaigns delayed to Q4): 15 entries
    FOR i IN 1..15 LOOP
        v_date := make_date(2024, GREATEST(1, i - 3), 10 + (i * 2) % 20);
        v_amount := CASE WHEN i <= 5 THEN 85000 + (random() * 15000)::INT
                         WHEN i <= 10 THEN 60000 + (random() * 10000)::INT
                         ELSE 35000 + (random() * 10000)::INT END;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Marketing campaign spend', 'mktg_ops', 'vp_mktg_jones', 'POSTED')
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES
            (v_entry_id, '5400', 3, v_amount, 0, 'Digital advertising'),
            (v_entry_id, '1000', 3, 0, v_amount, 'Cash payment');
    END LOOP;

    -- Misc operational expenses: 25 entries
    FOR i IN 1..25 LOOP
        v_date := make_date(2024, (i % 12) + 1, (i * 3) % 28 + 1);
        v_bu := (i % 6) + 1;
        v_amount := 15000 + (random() * 35000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Operational expense', 'ap_system', 'controller_park',
                CASE WHEN i = 22 THEN 'PENDING' WHEN i = 24 THEN 'REVERSED' ELSE 'POSTED' END)
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES
            (v_entry_id, CASE (i % 5) WHEN 0 THEN '5300' WHEN 1 THEN '5500' WHEN 2 THEN '5700' WHEN 3 THEN '5900' ELSE '6100' END,
             v_bu, v_amount, 0, 'Operating cost'),
            (v_entry_id, '2000', v_bu, 0, v_amount, 'Accounts payable');
    END LOOP;

    -- Pending/reversed entries for audit trail: 10 entries
    FOR i IN 1..10 LOOP
        v_date := make_date(2024, 10 + (i % 3), i + 5);
        v_amount := 50000 + (random() * 100000)::INT;
        INSERT INTO journal_entries (entry_date, description, created_by, approved_by, status)
        VALUES (v_date, 'Quarter-end adjustment #' || i, 'controller_park', NULL,
                CASE WHEN i <= 6 THEN 'PENDING' ELSE 'REVERSED' END)
        RETURNING entry_id INTO v_entry_id;
        INSERT INTO journal_lines (entry_id, account_id, business_unit_id, debit_amount, credit_amount, description)
        VALUES
            (v_entry_id, '2100', (i % 6) + 1, v_amount, 0, 'Accrual adjustment'),
            (v_entry_id, '5000', (i % 6) + 1, 0, v_amount, 'Salary accrual reversal');
    END LOOP;
END $$;

-- Budgets: FY2024 (6 BUs × 5 key expense accounts = 30 rows)
-- Story: Engineering Q3 budget was 3.2M but they spent ~4.5M (overspend)
--        Marketing annual budget 4.8M but only spent ~2.9M (underspend)
INSERT INTO budgets (fiscal_year, business_unit_id, account_id, q1_amount, q2_amount, q3_amount, q4_amount) VALUES
-- Engineering
(2024, 1, '5000', 3060000, 3060000, 3060000, 3060000),
(2024, 1, '5200', 800000, 850000, 900000, 950000),
(2024, 1, '5600', 200000, 200000, 250000, 250000),
(2024, 1, '5300', 150000, 150000, 175000, 175000),
(2024, 1, '5500', 75000, 80000, 85000, 90000),
-- Sales
(2024, 2, '5000', 1485000, 1485000, 1485000, 1485000),
(2024, 2, '5500', 200000, 250000, 275000, 300000),
(2024, 2, '5600', 100000, 100000, 125000, 125000),
(2024, 2, '5400', 50000, 60000, 70000, 80000),
(2024, 2, '5300', 80000, 80000, 90000, 90000),
-- Marketing
(2024, 3, '5000', 900000, 900000, 900000, 900000),
(2024, 3, '5400', 400000, 450000, 500000, 550000),
(2024, 3, '5600', 75000, 75000, 100000, 100000),
(2024, 3, '5500', 50000, 60000, 65000, 70000),
(2024, 3, '5300', 40000, 40000, 45000, 45000),
-- Operations
(2024, 4, '5000', 1620000, 1620000, 1620000, 1620000),
(2024, 4, '5200', 300000, 320000, 340000, 360000),
(2024, 4, '5700', 250000, 250000, 250000, 250000),
(2024, 4, '5300', 60000, 65000, 70000, 75000),
(2024, 4, '6100', 45000, 45000, 50000, 50000),
-- Finance
(2024, 5, '5000', 660000, 660000, 660000, 660000),
(2024, 5, '5600', 150000, 150000, 175000, 175000),
(2024, 5, '5300', 80000, 80000, 85000, 85000),
(2024, 5, '6000', 120000, 120000, 125000, 125000),
(2024, 5, '5900', 15000, 15000, 15000, 15000),
-- HR
(2024, 6, '5000', 405000, 405000, 405000, 405000),
(2024, 6, '5600', 60000, 60000, 75000, 75000),
(2024, 6, '5500', 30000, 35000, 40000, 45000),
(2024, 6, '5300', 25000, 25000, 30000, 30000),
(2024, 6, '5900', 10000, 10000, 12000, 12000);

-- Invoices (80): mix of PAID, OUTSTANDING, OVERDUE, DISPUTED
-- Sales BU has overdue invoices (story point)
INSERT INTO invoices (vendor_name, invoice_date, due_date, amount, status, business_unit_id, account_id, paid_date) VALUES
-- PAID invoices (40)
('Amazon Web Services',     '2024-01-15', '2024-02-14', 156000.00, 'PAID', 1, '5200', '2024-02-10'),
('Google Cloud Platform',   '2024-02-01', '2024-03-02', 142500.00, 'PAID', 1, '5200', '2024-02-28'),
('Salesforce Inc',          '2024-01-20', '2024-02-19', 89000.00,  'PAID', 2, '5300', '2024-02-15'),
('HubSpot',                 '2024-02-10', '2024-03-11', 34500.00,  'PAID', 3, '5400', '2024-03-08'),
('WeWork Offices',          '2024-01-01', '2024-01-31', 125000.00, 'PAID', 4, '5700', '2024-01-28'),
('Deloitte Consulting',     '2024-02-15', '2024-03-16', 275000.00, 'PAID', 5, '5600', '2024-03-14'),
('Workday Inc',             '2024-03-01', '2024-03-31', 67000.00,  'PAID', 6, '5300', '2024-03-29'),
('Datadog',                 '2024-03-10', '2024-04-09', 48000.00,  'PAID', 1, '5200', '2024-04-05'),
('LinkedIn Ads',            '2024-03-15', '2024-04-14', 92000.00,  'PAID', 3, '5400', '2024-04-10'),
('United Airlines',         '2024-04-01', '2024-04-30', 28500.00,  'PAID', 2, '5500', '2024-04-28'),
('Snowflake Computing',     '2024-04-15', '2024-05-14', 73000.00,  'PAID', 1, '5200', '2024-05-12'),
('Atlassian',               '2024-04-20', '2024-05-19', 45000.00,  'PAID', 1, '5300', '2024-05-15'),
('McKinsey & Co',           '2024-05-01', '2024-05-31', 380000.00, 'PAID', 5, '5600', '2024-05-28'),
('Google Ads',              '2024-05-10', '2024-06-09', 115000.00, 'PAID', 3, '5400', '2024-06-05'),
('Rackspace',               '2024-05-15', '2024-06-14', 62000.00,  'PAID', 4, '5200', '2024-06-10'),
('KPMG',                    '2024-06-01', '2024-06-30', 195000.00, 'PAID', 5, '5600', '2024-06-28'),
('AWS',                     '2024-06-15', '2024-07-14', 178000.00, 'PAID', 1, '5200', '2024-07-10'),
('DocuSign',                '2024-06-20', '2024-07-19', 22000.00,  'PAID', 6, '5300', '2024-07-15'),
('Marriott Hotels',         '2024-07-01', '2024-07-31', 31000.00,  'PAID', 2, '5500', '2024-07-28'),
('Meta Ads',                '2024-07-10', '2024-08-09', 78000.00,  'PAID', 3, '5400', '2024-08-05'),
('Stripe',                  '2024-07-15', '2024-08-14', 19500.00,  'PAID', 4, '5300', '2024-08-10'),
('PwC',                     '2024-08-01', '2024-08-31', 210000.00, 'PAID', 5, '5600', '2024-08-28'),
('AWS',                     '2024-08-15', '2024-09-14', 195000.00, 'PAID', 1, '5200', '2024-09-10'),
('Cisco Systems',           '2024-08-20', '2024-09-19', 88000.00,  'PAID', 4, '5300', '2024-09-15'),
('Hilton Hotels',           '2024-09-01', '2024-09-30', 42000.00,  'PAID', 2, '5500', '2024-09-28'),
('GCP',                     '2024-09-10', '2024-10-09', 167000.00, 'PAID', 1, '5200', '2024-10-05'),
('Accenture',               '2024-09-15', '2024-10-14', 320000.00, 'PAID', 1, '5600', '2024-10-10'),
('Twitter/X Ads',           '2024-09-20', '2024-10-19', 45000.00,  'PAID', 3, '5400', '2024-10-15'),
('Oracle',                  '2024-10-01', '2024-10-31', 135000.00, 'PAID', 4, '5300', '2024-10-28'),
('EY',                      '2024-10-10', '2024-11-09', 185000.00, 'PAID', 5, '5600', '2024-11-05'),
('WeWork Offices',          '2024-10-01', '2024-10-31', 125000.00, 'PAID', 4, '5700', '2024-10-28'),
('Zoom',                    '2024-10-15', '2024-11-14', 28000.00,  'PAID', 6, '5300', '2024-11-10'),
('Delta Airlines',          '2024-10-20', '2024-11-19', 36500.00,  'PAID', 2, '5500', '2024-11-15'),
('AWS',                     '2024-11-01', '2024-11-30', 205000.00, 'PAID', 1, '5200', '2024-11-27'),
('Gartner',                 '2024-11-05', '2024-12-04', 95000.00,  'PAID', 5, '5600', '2024-12-02'),
('JetBrains',               '2024-11-10', '2024-12-09', 38000.00,  'PAID', 1, '5300', '2024-12-05'),
('GitHub Enterprise',       '2024-11-15', '2024-12-14', 52000.00,  'PAID', 1, '5300', '2024-12-10'),
('Cushman & Wakefield',     '2024-11-01', '2024-11-30', 95000.00,  'PAID', 4, '5700', '2024-11-25'),
('ADP Payroll',             '2024-11-20', '2024-12-19', 42000.00,  'PAID', 6, '5600', '2024-12-15'),
('Slack Technologies',      '2024-12-01', '2024-12-31', 31000.00,  'PAID', 1, '5300', '2024-12-28'),
-- OUTSTANDING invoices (20)
('AWS',                     '2024-11-15', '2024-12-14', 215000.00, 'OUTSTANDING', 1, '5200', NULL),
('Snowflake Computing',     '2024-11-20', '2024-12-19', 89000.00,  'OUTSTANDING', 1, '5200', NULL),
('Google Ads',              '2024-11-25', '2024-12-24', 105000.00, 'OUTSTANDING', 3, '5400', NULL),
('Salesforce Inc',          '2024-12-01', '2024-12-31', 89000.00,  'OUTSTANDING', 2, '5300', NULL),
('Deloitte Consulting',     '2024-12-05', '2025-01-03', 290000.00, 'OUTSTANDING', 5, '5600', NULL),
('Datadog',                 '2024-12-10', '2025-01-08', 52000.00,  'OUTSTANDING', 1, '5200', NULL),
('WeWork Offices',          '2024-12-01', '2024-12-31', 125000.00, 'OUTSTANDING', 4, '5700', NULL),
('LinkedIn Ads',            '2024-12-08', '2025-01-06', 68000.00,  'OUTSTANDING', 3, '5400', NULL),
('Workday Inc',             '2024-12-10', '2025-01-08', 67000.00,  'OUTSTANDING', 6, '5300', NULL),
('Atlassian',               '2024-12-12', '2025-01-10', 45000.00,  'OUTSTANDING', 1, '5300', NULL),
('Cisco Systems',           '2024-12-15', '2025-01-13', 72000.00,  'OUTSTANDING', 4, '5300', NULL),
('HubSpot',                 '2024-12-15', '2025-01-13', 34500.00,  'OUTSTANDING', 3, '5400', NULL),
('JetBrains',               '2024-12-18', '2025-01-16', 38000.00,  'OUTSTANDING', 1, '5300', NULL),
('Zoom',                    '2024-12-18', '2025-01-16', 28000.00,  'OUTSTANDING', 6, '5300', NULL),
('GitHub Enterprise',       '2024-12-20', '2025-01-18', 52000.00,  'OUTSTANDING', 1, '5300', NULL),
('Rackspace',               '2024-12-20', '2025-01-18', 58000.00,  'OUTSTANDING', 4, '5200', NULL),
('ADP Payroll',             '2024-12-22', '2025-01-20', 42000.00,  'OUTSTANDING', 6, '5600', NULL),
('Oracle',                  '2024-12-22', '2025-01-20', 135000.00, 'OUTSTANDING', 4, '5300', NULL),
('Meta Ads',                '2024-12-23', '2025-01-21', 82000.00,  'OUTSTANDING', 3, '5400', NULL),
('Stripe',                  '2024-12-23', '2025-01-21', 19500.00,  'OUTSTANDING', 4, '5300', NULL),
-- OVERDUE invoices (12) — Sales BU has most overdue (story)
('Accenture',               '2024-08-01', '2024-09-01', 450000.00, 'OVERDUE', 2, '5600', NULL),
('McKinsey & Co',           '2024-08-15', '2024-09-14', 325000.00, 'OVERDUE', 2, '5600', NULL),
('SAP Concur',              '2024-09-01', '2024-09-30', 78000.00,  'OVERDUE', 2, '5300', NULL),
('Hilton Hotels',           '2024-09-10', '2024-10-09', 55000.00,  'OVERDUE', 2, '5500', NULL),
('United Airlines',         '2024-09-15', '2024-10-14', 42000.00,  'OVERDUE', 2, '5500', NULL),
('AWS',                     '2024-09-20', '2024-10-19', 185000.00, 'OVERDUE', 1, '5200', NULL),
('GCP',                     '2024-10-01', '2024-10-31', 142000.00, 'OVERDUE', 1, '5200', NULL),
('Marriott Hotels',         '2024-10-05', '2024-11-04', 38000.00,  'OVERDUE', 2, '5500', NULL),
('Delta Airlines',          '2024-10-10', '2024-11-09', 29500.00,  'OVERDUE', 2, '5500', NULL),
('Google Ads',              '2024-10-15', '2024-11-14', 95000.00,  'OVERDUE', 3, '5400', NULL),
('Gartner',                 '2024-10-20', '2024-11-19', 85000.00,  'OVERDUE', 5, '5600', NULL),
('Twitter/X Ads',           '2024-10-25', '2024-11-24', 52000.00,  'OVERDUE', 3, '5400', NULL),
-- DISPUTED invoices (8)
('Accenture',               '2024-07-01', '2024-07-31', 180000.00, 'DISPUTED', 1, '5600', NULL),
('McKinsey & Co',           '2024-07-15', '2024-08-14', 240000.00, 'DISPUTED', 2, '5600', NULL),
('Rackspace',               '2024-08-01', '2024-08-31', 95000.00,  'DISPUTED', 4, '5200', NULL),
('Oracle',                  '2024-08-20', '2024-09-19', 165000.00, 'DISPUTED', 4, '5300', NULL),
('Deloitte Consulting',     '2024-09-01', '2024-09-30', 310000.00, 'DISPUTED', 5, '5600', NULL),
('SAP Concur',              '2024-09-15', '2024-10-14', 62000.00,  'DISPUTED', 2, '5300', NULL),
('Cushman & Wakefield',     '2024-10-01', '2024-10-31', 145000.00, 'DISPUTED', 4, '5700', NULL),
('EY',                      '2024-10-15', '2024-11-14', 220000.00, 'DISPUTED', 5, '5600', NULL);

-- Cash Flow (50 entries across FY2024)
INSERT INTO cash_flow (flow_date, flow_type, description, amount, business_unit_id) VALUES
-- OPERATING (30 entries)
('2024-01-15', 'OPERATING', 'Customer collections - product sales',    2150000, 2),
('2024-01-31', 'OPERATING', 'Payroll disbursement',                   -2710000, 5),
('2024-02-15', 'OPERATING', 'Customer collections - subscriptions',    1050000, 1),
('2024-02-28', 'OPERATING', 'Vendor payments - cloud services',        -485000, 1),
('2024-03-15', 'OPERATING', 'Customer collections - services',          720000, 4),
('2024-03-31', 'OPERATING', 'Payroll disbursement',                   -2710000, 5),
('2024-04-15', 'OPERATING', 'Customer collections - product sales',    2280000, 2),
('2024-04-30', 'OPERATING', 'Vendor payments - marketing',             -210000, 3),
('2024-05-15', 'OPERATING', 'Customer collections - consulting',        340000, 2),
('2024-05-31', 'OPERATING', 'Payroll disbursement',                   -2710000, 5),
('2024-06-15', 'OPERATING', 'Customer collections - subscriptions',    1120000, 1),
('2024-06-30', 'OPERATING', 'Vendor payments - professional svcs',     -580000, 5),
('2024-07-15', 'OPERATING', 'Customer collections - product sales',    1950000, 2),
('2024-07-31', 'OPERATING', 'Payroll disbursement',                   -2710000, 5),
('2024-08-15', 'OPERATING', 'Customer collections - all streams',      3200000, 2),
('2024-08-31', 'OPERATING', 'Vendor payments - cloud surge',           -890000, 1),
('2024-09-15', 'OPERATING', 'Customer collections - services',          680000, 4),
('2024-09-30', 'OPERATING', 'Payroll disbursement',                   -2710000, 5),
('2024-10-15', 'OPERATING', 'Customer collections - product sales',    2400000, 2),
('2024-10-31', 'OPERATING', 'Vendor payments - contractors',           -420000, 1),
('2024-11-15', 'OPERATING', 'Customer collections - subscriptions',    1080000, 1),
('2024-11-30', 'OPERATING', 'Payroll disbursement',                   -2710000, 5),
('2024-12-15', 'OPERATING', 'Customer collections - all streams',      3500000, 2),
('2024-12-31', 'OPERATING', 'Year-end vendor settlements',            -1250000, 5),
-- INVESTING (12 entries)
('2024-01-20', 'INVESTING', 'Server hardware purchase',                -350000, 1),
('2024-03-10', 'INVESTING', 'Office buildout - APAC expansion',        -890000, 4),
('2024-04-25', 'INVESTING', 'Software IP acquisition',                 -425000, 1),
('2024-06-15', 'INVESTING', 'Lab equipment - QA automation',           -180000, 1),
('2024-07-20', 'INVESTING', 'Sale of deprecated equipment',             120000, 4),
('2024-08-30', 'INVESTING', 'Data center expansion',                   -650000, 4),
('2024-09-15', 'INVESTING', 'Security infrastructure upgrade',         -275000, 1),
('2024-10-10', 'INVESTING', 'Office furniture - new hires',            -95000,  6),
('2024-11-05', 'INVESTING', 'Network infrastructure upgrade',          -310000, 4),
('2024-11-20', 'INVESTING', 'Sale of old office lease',                 450000, 4),
('2024-12-01', 'INVESTING', 'AI/ML compute cluster',                   -520000, 1),
('2024-12-15', 'INVESTING', 'Patent portfolio acquisition',            -380000, 1),
-- FINANCING (8 entries)
('2024-02-01', 'FINANCING', 'Credit line drawdown',                    2000000, 5),
('2024-03-15', 'FINANCING', 'Dividend payment',                        -750000, 5),
('2024-05-01', 'FINANCING', 'Credit line repayment',                  -1000000, 5),
('2024-06-30', 'FINANCING', 'Dividend payment',                        -750000, 5),
('2024-08-15', 'FINANCING', 'Term loan proceeds',                      5000000, 5),
('2024-09-30', 'FINANCING', 'Dividend payment',                        -750000, 5),
('2024-11-01', 'FINANCING', 'Credit line repayment',                  -1000000, 5),
('2024-12-31', 'FINANCING', 'Dividend payment',                        -750000, 5);

-- =============================================================================
-- Read-Only Role
-- =============================================================================
CREATE USER datasquire_reader WITH PASSWORD 'reader_pw';
GRANT CONNECT ON DATABASE financedb TO datasquire_reader;
GRANT USAGE ON SCHEMA public TO datasquire_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO datasquire_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO datasquire_reader;
ALTER ROLE datasquire_reader SET statement_timeout = '30s';
