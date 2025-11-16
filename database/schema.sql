-- Database schema for fake data generation project
-- Run this to set up your PostgreSQL database

-- Create database (run as postgres superuser)
-- CREATE DATABASE fakedata;
-- \c fakedata

-- Drop existing tables if they exist
DROP TABLE IF EXISTS proposals CASCADE;
DROP TABLE IF EXISTS activity_logs CASCADE;

-- Activity Logs table
-- Stores activity log entries with user and business context
CREATE TABLE activity_logs (
    id INTEGER PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    user_id INTEGER NOT NULL,
    industry_name VARCHAR(100) NOT NULL,
    tenant_location VARCHAR(200) NOT NULL,
    success_probability INTEGER NOT NULL CHECK (success_probability BETWEEN 0 AND 100),
    account_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Proposals table
-- Stores proposals linked to activity logs
CREATE TABLE proposals (
    id INTEGER PRIMARY KEY,
    activity_log_id INTEGER NOT NULL,
    last_entered BOOLEAN NOT NULL DEFAULT FALSE,
    edit_locked BOOLEAN NOT NULL DEFAULT FALSE,
    proposal_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint to ensure referential integrity
    CONSTRAINT fk_activity_log
        FOREIGN KEY (activity_log_id)
        REFERENCES activity_logs(id)
        ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_account_id ON activity_logs(account_id);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);
CREATE INDEX idx_proposals_activity_log_id ON proposals(activity_log_id);
CREATE INDEX idx_proposals_created_at ON proposals(created_at);

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON TABLE activity_logs TO your_user;
-- GRANT ALL PRIVILEGES ON TABLE proposals TO your_user;

-- Verify tables were created
SELECT
    table_name,
    table_type
FROM
    information_schema.tables
WHERE
    table_schema = 'public'
    AND table_name IN ('activity_logs', 'proposals');
