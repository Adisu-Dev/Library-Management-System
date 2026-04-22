
USE LibraryDB;
GO


IF OBJECT_ID('BorrowRecords', 'U') IS NOT NULL DROP TABLE BorrowRecords;
IF OBJECT_ID('Books', 'U') IS NOT NULL DROP TABLE Books;
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
GO

-- create users table
CREATE TABLE Users (
    UserID INT PRIMARY KEY IDENTITY(1,1),
    FullName NVARCHAR(100) NOT NULL,
    Username NVARCHAR(50) UNIQUE NOT NULL,
    PasswordHash NVARCHAR(255) NOT NULL,
    Role NVARCHAR(20) NOT NULL, -- 'Admin', 'Librarian', 'Student'
    Phone NVARCHAR(20)
);
GO

-- create books table
CREATE TABLE Books (
    BookID INT PRIMARY KEY IDENTITY(1,1),
    Title NVARCHAR(200) NOT NULL,
    Author NVARCHAR(100),
    ISBN NVARCHAR(50),
    Category NVARCHAR(50),
    Quantity INT DEFAULT 0,
    AvailableQuantity INT DEFAULT 0,
    AddedDate DATETIME DEFAULT GETDATE()
);
GO

-- create borrows record
CREATE TABLE BorrowRecords (
    RecordID INT PRIMARY KEY IDENTITY(1,1),
    BookID INT FOREIGN KEY REFERENCES Books(BookID),
    UserID INT FOREIGN KEY REFERENCES Users(UserID),
    IssueDate DATETIME DEFAULT GETDATE(),
    DueDate DATETIME NOT NULL,
    ReturnDate DATETIME NULL,
    PenaltyAmount DECIMAL(10,2) DEFAULT 0.00
);
GO

-- first to enter admin password
INSERT INTO Users (FullName, Username, PasswordHash, Role, Phone) 
VALUES ('System Admin', 'admin', 'admin123', 'Admin', '0911000000');
GO

-- to cheek they are properly created
SELECT * FROM Users;
SELECT * FROM Books;
SELECT UserID, FullName, Username, PasswordHash, Role FROM Users;

GO

INSERT INTO Books (Title, Author, ISBN, Category, Quantity, AvailableQuantity)
VALUES 
('Dertogada', 'Yismake Worku', '978-BDU-01', 'Fiction', 10, 10),
('Advanced Java', 'Adisu Dereje', '978-BDU-02', 'Technology', 5, 5),
('Sememen', 'Sisay Nigusu', '978-BDU-03', 'Fiction', 12, 12);
GO

SELECT * FROM Books; -- መግባታቸውን እዚህ ጋር ማየት ትችላለህ
SELECT * FROM Users;

INSERT INTO Users (FullName, Username, PasswordHash, Role) 
VALUES ('Adisu Dereje', 'BDU501', '123456', 'Student');

INSERT INTO Books (Title, Author, AvailableQuantity) 
VALUES ('Advanced Java', 'Oracle', 10);

CREATE TABLE Reservations (
    ReservationID INT IDENTITY PRIMARY KEY,
    BookID INT FOREIGN KEY REFERENCES Books(BookID),
    UserID INT FOREIGN KEY REFERENCES Users(UserID),
    ReservedAt DATETIME DEFAULT GETDATE(),
    Status NVARCHAR(20) DEFAULT 'Pending'
);

