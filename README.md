# AI-Native Core Service

## 1. Project Purpose
This repository serves as the baseline foundation for the Core Service API. It is architected under the AI-Native SDLC paradigm, utilizing GitHub Copilot for spec-driven generation and gated quality verification.

## 2. Technology Stack & Prerequisites
- **Language / Runtime:** Python 3.11+ / Node.js 20+ / .NET 8 / Java 21 / C++20 (Select your project primary stack)
- **Framework:** REST API (FastAPI / Express / ASP.NET Core / Spring Boot)
- **Database:** PostgreSQL 16
- **Tooling:** Docker, GitHub Copilot Extension, PyTest / Jest

## 3. Getting Started & How to Run
```bash
# Step 1: Clone the repository
git clone https://github.com/your-org/ai-native-core.git
cd ai-native-core

# Step 2: Install dependencies
pip install -r requirements.txt  # or npm install / dotnet restore

# Step 3: Run local developer server
python src/main.py               # or npm start / dotnet run
```

## 4. Repository Governance
- All feature additions must originate from an approved GitHub Issue.
- AI-generated code must strictly pass unit tests and human peer review before merging.
