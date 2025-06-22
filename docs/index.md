# DS-Sim Documentation Index

Welcome to the DS-Sim (Distributed Systems Simulator) documentation. This index provides easy navigation to all available documentation.

## Architecture & Design

- [**architecture.md**](architecture.md) - System architecture overview with component diagrams and design patterns
- [**architecture-diagrams.puml**](architecture-diagrams.puml) - PlantUML source for architecture diagrams
- [**developer-guide.md**](developer-guide.md) - Comprehensive guide for developers working with DS-Sim

## Testing

- [**testing-guide.md**](testing-guide.md) - Complete testing guide including unit tests and protocol simulations
- [**test-infrastructure.md**](test-infrastructure.md) - Details about the headless testing infrastructure
- [**message-count-verification.md**](message-count-verification.md) - Guide to message counting and verification in tests

## GUI Decoupling Project

- [**gui-decoupling-summary.md**](gui-decoupling-summary.md) - Overview of the GUI decoupling initiative
- [**gui-decoupling-plan.md**](gui-decoupling-plan.md) - Original plan for decoupling GUI from core logic
- [**gui-decoupling-status.md**](gui-decoupling-status.md) - Current status of GUI decoupling implementation
- [**decoupling-implementation-guide.md**](decoupling-implementation-guide.md) - Implementation details and patterns

## Features & Frameworks

- [**simulation-builder-framework.md**](simulation-builder-framework.md) - Programmatic simulation creation without GUI
- [**timestamp-events-guide.md**](timestamp-events-guide.md) - Guide to implementing timestamp-triggered events

## Technical Notes

- [**build-fixes-summary.md**](build-fixes-summary.md) - Summary of build fixes and Maven configuration
- [**serialization-notes.txt**](serialization-notes.txt) - Notes on DS-Sim's custom serialization format

## Quick Links

### For New Users
1. Start with [architecture.md](architecture.md) to understand the system
2. Read [developer-guide.md](developer-guide.md) for development setup
3. Check [testing-guide.md](testing-guide.md) to run and write tests

### For Contributors
1. Review [gui-decoupling-summary.md](gui-decoupling-summary.md) for recent architectural changes
2. See [test-infrastructure.md](test-infrastructure.md) for headless testing patterns
3. Use [simulation-builder-framework.md](simulation-builder-framework.md) to create test simulations

### For Protocol Developers
1. Study [developer-guide.md](developer-guide.md) for protocol implementation patterns
2. Learn about [timestamp-events-guide.md](timestamp-events-guide.md) for event handling
3. Follow [testing-guide.md](testing-guide.md) to test your protocol

## Documentation Standards

- All documentation files use lowercase names with hyphens (e.g., `developer-guide.md`)
- Markdown files should include clear headings and code examples
- Technical diagrams use PlantUML format for maintainability
- Each document should be self-contained but reference related docs

## Contributing to Documentation

When adding new documentation:
1. Use lowercase filenames with `.md` extension
2. Add an entry to this index with a brief description
3. Include the document in the appropriate section
4. Ensure cross-references to other docs are accurate

---

*Last updated: December 2024*