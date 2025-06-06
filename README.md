# DS-Sim

DS-Sim is a modern, open-source simulator for distributed systems, written in Java. It provides a powerful environment for simulating and learning about distributed systems concepts.

## Features

- Protocol simulation
- Event handling
- Lamport and Vector time implementations
- Modern Java-based architecture
- Interactive GUI using JavaFX
- Comprehensive logging and monitoring
- JSON-based configuration

## Requirements

- Java 17 or higher
- Maven 3.8 or higher

## Building

```bash
# Clone the repository
git clone https://github.com/yourusername/ds-sim.git
cd ds-sim

# Build the project
mvn clean package

# Run the simulator
java -jar target/ds-sim-1.0-SNAPSHOT.jar
```

## Development

```bash
# Run tests
mvn test

# Generate documentation
mvn javadoc:javadoc
```

## Project Structure

```
ds-sim/
├── src/
│   ├── main/
│   │   ├── java/        # Source code
│   │   └── resources/   # Configuration files
│   └── test/
│       ├── java/        # Test code
│       └── resources/   # Test resources
├── docs/                # Documentation
└── pom.xml             # Project configuration
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the terms of the license included in the repository.

## Acknowledgments

- Original VS-Sim project: https://codeberg.org/snonux/vs-sim/
