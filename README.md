
# Safe Spend: Personal Finance Tracker

Safe spend is a comprehensive, personal finance management application for Android. Built with Kotlin and Firebase Realtime Database, it provides users with a powerful suite of tools to track their income and expenses, manage multiple accounts, set financial goals, and visualize their spending habits.


## Table of Contents

- [Features](#features)
- [Technical Stack](#technical-stack)
- [Core Architecture](#core-architecture)
  - [Transaction-Based Accounting](#transaction-based-accounting)
  - [Data Synchronization](#data-synchronization)

## Features

- **User Authentication**: Secure manual sign-up and sign-in system.
- **Account Management**: Create and manage multiple financial accounts (e.g., Bank, Savings), each with a unique color identifier.
- **Transaction Tracking**: Log income and expense transactions with descriptions, amounts, categories, and optional receipt photo attachments.
- **Category Management**: Customizable income and expense categories with icons.
- **Fund Transfers**: Seamlessly transfer funds between accounts, with balances updated in real-time.
- **Financial Goals**: Set monthly spending or savings goals for specific accounts. The app automatically tracks progress based on relevant transactions.
- **Reporting & Visualization**:
  - **Dashboard**: A central home screen showing total balance, monthly income/expense summary, and all accounts.
  - **Transaction Reports**: A detailed line chart visualizing cumulative income, expenses, and spending limits over custom date ranges (7 days, 30 days, custom).
  - **Account Details**: A radial chart showing the income vs. expense breakdown for a single account.

## Technical Stack

- **Language**: Kotlin (100%)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Firebase Realtime Database for live data synchronization and offline persistence.
- **UI**: Android Views with ViewBinding.
- **Charting**: MPAndroidChart for dynamic line, and radial charts.
- **Asynchronous Programming**: Kotlin Coroutines for managing background tasks and database operations.
- **Camera**: AndroidX CameraX library for a modern, reliable camera integration.

## Core Architecture

The application is built on modern Android architecture principles to ensure it is robust, scalable, and maintainable.

### MVVM (Model-View-ViewModel)

- **View (Activities/Fragments)**: Responsible for observing data from the ViewModel and displaying it to the user. It captures user input and forwards it to the ViewModel.
- **ViewModel**: Acts as a bridge between the View and the Repository. It holds UI-related data in a lifecycle-conscious way (LiveData), making it survive configuration changes. It does not know about the View.
- **Repository**: The single source of truth for all application data. It abstracts the data source (Firebase) from the rest of the app. All ViewModels interact with the Repository to fetch or save data.

### Transaction-Based Accounting

A key architectural decision in this project is that an account's balance is not stored as a static value. Instead, it is always calculated dynamically as the sum of all its associated transactions (balance = Σ transactions). This creates a robust, self-correcting system where the balance is always accurate and verifiable.


### Data Synchronization

To ensure a seamless offline experience and prevent data contention, the app employs several Firebase features:

1. **Disk Persistence**: `Firebase.database.setPersistenceEnabled(true)` is called at app startup, enabling the SDK to cache all data on the device.
2. **Proactive Syncing (keepSynced)**: After a user logs in, the app instructs Firebase to keep the accounts, transactions, and goals nodes actively synchronized.
3. **Atomic Operations (runTransaction)**: Critical operations like fund transfers are performed within a Firebase Transaction to prevent race conditions and ensure data integrity, even with multiple users or devices.
4. **"Warm-Up" Reads**: Before critical transactions, a `.get().await()` call is performed to ensure the local cache is fully synced with the server, preventing errors on the first attempt after an app start.
