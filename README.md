# Expense Split Tracker

A comprehensive Spring Boot application for tracking and splitting expenses among groups of users.

## Problem Statement
Build an **Expense Split Tracker** that allows users to create groups, add expenses with different split methods, track balances, settle debts, and simplify transactions.

## Approach
- Developed using **Spring Boot (Java 21)** and **MySQL 8.0**.
- Followed MVC pattern: Controller → Service → Repository → Entity.
- Used **BigDecimal** for all currency calculations to ensure precision.
- Implemented:
    - Equal, Exact, and Percentage splits.
    - Balance tracking and debt settlements.
    - Debt simplification using a **greedy algorithm**.
- REST APIs are fully testable via **Postman collection**.

## Features
✅ Create and manage groups  
✅ Add expenses with multiple split types (Equal, Exact, Percentage)  
✅ Track balances between users  
✅ Settle debts  
✅ Simplify debts automatically  
✅ Validation for currency and settlements  
✅ Transaction history

## Prerequisites
- Java 21
- Maven 3.6+
- MySQL 8.0+

## Setup Instructions

1. **Database Setup**
   ```sql
   CREATE DATABASE expense_tracker;
