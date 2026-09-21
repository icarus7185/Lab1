# Developer Contribution Guidelines

Welcome! To maintain code quality and safety when working alongside AI collaborators, please strictly follow these rules:

## 1. Branch Naming Conventions
- Features: `feature/WO-<issue_id>-<short-description>`
- Bug Fixes: `fix/WO-<issue_id>-<short-description>`
- Spikes / Research: `spike/WO-<issue_id>-<short-description>`

## 2. Pull Request Rules
1. Never push code directly to `main` or `develop`.
2. Every PR must map to an active GitHub Issue.
3. Automated CI checks (tests, linter, secret scanner) must pass completely.
4. Requires at least one approval from a human Tech Lead / Peer Reviewer.

## 3. Mandatory AI Output Policy ("AI Output Must Be Reviewed")
- **Untrusted Code Policy:** All code produced or suggested by GitHub Copilot/AI tools is classified as **UNTRUSTED** until verified by a human developer.
- **Zero Hallucinations:** Verify that imported packages, internal API calls, and logic exist and conform to domain models.
- **No Secrets in Prompts:** Do NOT include production keys, database credentials, or proprietary client data in Copilot prompts or repo files.
