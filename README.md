# GROW21 - Child Progress Tracking DBMS 🚀

GROW21 is a professional Database Management System (DBMS) designed for special education centers. It enables educators and parents to track child development through structured learning modules, session monitoring, and automated progress reporting.

![Theme Preview](https://img.shields.io/badge/Theme-Dark--Gold-gold)
![DB](https://img.shields.io/badge/Database-MySQL-blue)
![Backend](https://img.shields.io/badge/Backend-Node.js-green)
![Frontend](https://img.shields.io/badge/Frontend-React-blue)

## 🌟 Key Features
- **3NF Normalized Schema**: Optimized database design for zero data redundancy.
- **Automated Reporting**: Uses **MySQL Stored Procedures** to calculate skill mastery on-the-fly.
- **Advanced Querying**: Implements **CTEs (Common Table Expressions)**, Joins, and Subqueries for deep data insights.
- **Transaction Safety**: ACID-compliant transactions for child enrollment and session logging.
- **Premium UI**: Sleek Dark-Gold aesthetic built with React and Vanilla CSS.

## 🛠️ Tech Stack
- **Database**: MySQL 8.0+
- **Backend**: Node.js, Express.js, MySQL2
- **Frontend**: React (Vite), React Router
- **Design**: Premium Glassmorphism & Custom CSS variables

## 🚀 Getting Started

### 1. Database Setup
1. Open MySQL Workbench.
2. Run the `Grow21.sql` script to create the schema and seed data.
3. This will create the `Grow21_DB` with all required Views, Procedures, and Indexes.

### 2. Backend Setup
```bash
cd backend
npm install
# Update db.js with your MySQL password
npm start
```

### 3. Frontend Setup
```bash
cd frontend
npm install
npm run dev
```
The app will be live at `http://localhost:5173`.

## 📊 Database Architecture
The project demonstrates core DBMS concepts:
- **Keys**: Primary, Foreign, and Candidate keys properly implemented.
- **Normalization**: Schema follows 1NF, 2NF, and 3NF.
- **Relationships**: 1:1 (Parent-Child), 1:N (School-Educator), and M:N (Child-Educator via Bridge Table).
- **Optimization**: Explicit B-Tree indexing on foreign keys.

## 📄 License
This project is licensed under the MIT License.
