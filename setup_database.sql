-- ============================================================
-- ImageXposed Database Setup Script
-- Run this in SQL Server Management Studio (SSMS)
-- ============================================================

-- 1. Create the database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'ImageXposedDB')
BEGIN
    CREATE DATABASE ImageXposedDB;
END
GO

USE ImageXposedDB;
GO

-- 2. Users table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        username        VARCHAR(50)     PRIMARY KEY,
        email           VARCHAR(100)    NOT NULL UNIQUE,
        password_hash   VARCHAR(255)    NOT NULL,
        created_at      DATETIME2       DEFAULT GETDATE(),
        updated_at      DATETIME2       DEFAULT GETDATE()
    );
END
GO

-- 3. Images table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Images')
BEGIN
    CREATE TABLE Images (
        id              VARCHAR(100)    PRIMARY KEY,
        file_name       VARCHAR(255)    NOT NULL,
        file_format     VARCHAR(20)     NOT NULL,
        file_size       BIGINT          NOT NULL,
        file_path       VARCHAR(500)    NOT NULL,
        tracking_id     VARCHAR(100),
        created_at      DATETIME2       DEFAULT GETDATE()
    );
END
GO

-- 4. Scans table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Scans')
BEGIN
    CREATE TABLE Scans (
        id                  VARCHAR(100)    PRIMARY KEY,
        user_id             VARCHAR(50)     NOT NULL,
        image_id            VARCHAR(100)    NOT NULL,
        validation_result   VARCHAR(50),
        analysis_verdict    VARCHAR(50),
        analysis_confidence VARCHAR(20),
        md5_hash            VARCHAR(64),
        sha256_hash         VARCHAR(128),
        metadata_info       NVARCHAR(MAX),
        ela_result          NVARCHAR(MAX),
        scan_timestamp      DATETIME2       DEFAULT GETDATE(),

        FOREIGN KEY (user_id)  REFERENCES Users(username),
        FOREIGN KEY (image_id) REFERENCES Images(id)
    );
END
GO

-- 5. Reports table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Reports')
BEGIN
    CREATE TABLE Reports (
        id              VARCHAR(100)    PRIMARY KEY,
        scan_id         VARCHAR(100)    NOT NULL,
        report_type     VARCHAR(20),
        content         NVARCHAR(MAX),
        created_at      DATETIME2       DEFAULT GETDATE(),

        FOREIGN KEY (scan_id) REFERENCES Scans(id)
    );
END
GO

-- 6. Feedbacks table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Feedbacks')
BEGIN
    CREATE TABLE Feedbacks (
        id              VARCHAR(100)    PRIMARY KEY,
        user_id         VARCHAR(50)     NOT NULL,
        rating          INT,
        comments        NVARCHAR(MAX),
        created_at      DATETIME2       DEFAULT GETDATE()
    );
END
GO

-- 7. Seed default admin user (password: admin123)
-- The hash is SHA-256 of "admin123"
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'admin')
BEGIN
    INSERT INTO Users (username, email, password_hash)
    VALUES ('admin', 'admin@imagexposed.com',
            '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');
END
GO

PRINT '=== ImageXposedDB setup complete ===';
GO
