# Contributing to Payment Service

First off, thank you for considering contributing to this project! 🎉

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples**
- **Describe the behavior you observed and what you expected**
- **Include logs and error messages**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Use a clear and descriptive title**
- **Provide a detailed description of the suggested enhancement**
- **Explain why this enhancement would be useful**
- **List any additional context**

### Pull Requests

1. Fork the repo and create your branch from `main`
2. If you've added code, add tests
3. Ensure the test suite passes
4. Make sure your code follows the existing style
5. Write a clear commit message
6. Open a pull request with a clear title and description

## Development Setup
```bash
# Clone your fork
git clone https://github.com/your-username/payment-service.git

# Add upstream remote
git remote add upstream https://github.com/original-owner/payment-service.git

# Create a branch
git checkout -b feature/your-feature-name

# Make your changes and commit
git add .
git commit -m "Description of changes"

# Push to your fork
git push origin feature/your-feature-name
```

## Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc comments for public methods
- Keep methods small and focused
- Write unit tests for new features

## Commit Messages

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters
- Reference issues and pull requests

Examples:
```
Add payment retry mechanism
Fix duplicate payment bug in idempotency check
Update README with API examples
```

## Questions?

Feel free to open an issue with the "question" label!

Thank you for contributing! 🚀