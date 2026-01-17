# GitHub Copilot Commit Instructions

This repository follows the **[Conventional Commits 1.0.0 specification](https://www.conventionalcommits.org/en/v1.0.0/)**.  
All commit messages **MUST** conform to the rules below so we can maintain a clear commit history, generate changelogs, and automate versioning.

## Commit Message Format

```
<type>[optional scope][!]: <short description>

[optional body]

[optional footer(s)]
```

### Examples
- `feat: add user login API`
- `fix(parser): handle null values correctly`
- `docs: update installation guide`
- `chore!: drop support for Node 16`

```
fix: prevent race condition in request handling

Introduce request ID and reference to latest request.
Ignore responses from outdated requests.

BREAKING CHANGE: timeout configuration option was removed.
```

---

## Allowed Types

- **feat**: a new feature (correlates with `MINOR` in SemVer).
- **fix**: a bug fix (correlates with `PATCH` in SemVer).
- **docs**: documentation changes only.
- **style**: formatting changes, no code logic impact (e.g., whitespace, semicolons).
- **refactor**: code change that neither fixes a bug nor adds a feature.
- **perf**: performance improvement.
- **test**: add or update tests only.
- **build**: changes to build system or dependencies.
- **ci**: CI/CD configuration or scripts.
- **chore**: other changes that don’t affect `src` or `test`.
- **revert**: revert a previous commit.

---

## Breaking Changes

Breaking changes **MUST** be indicated by:
- Adding a `!` after the type/scope  
  Example:
  ```
  feat(api)!: update response format
  ```
- OR adding a `BREAKING CHANGE:` footer  
  Example:
  ```
  fix: adjust config loading order

  BREAKING CHANGE: environment variables now override config files
  ```

---

## Commit Body (Optional)

- Provide additional context if needed.
- Start body **after a blank line**.
- Use multiple paragraphs if necessary.

---

## Commit Footers (Optional)

- Follow [git trailer format](https://git-scm.com/docs/git-interpret-trailers).
- Examples:
  ```
  Reviewed-by: Alice
  Refs: #123
  ```

---

## Instructions for GitHub Copilot

When generating commit messages with Copilot:
1. **Always start with `<type>`** (e.g., feat, fix, docs).
2. **Keep description short (≤72 characters)**.
3. Add body text if explaining “why” is important.
4. Add `BREAKING CHANGE:` footer if necessary.
5. Use correct casing and punctuation consistently.

---

✅ Following these rules ensures:
- Automated **changelog generation**.
- Proper **semantic versioning** (SemVer).
- Easier collaboration and contribution.

