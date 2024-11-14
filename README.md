# artemis-cli

A CLI tool for artemis that implements common use-cases
that require interaction with the artemis API.


## Features

The tool supports the following use-cases:
- [x] Clone one or more programming submission
- [x] Automatically grade valid text submissions that match a given regex
- [x] List currently locked submissions
- [x] Unlock all or specific locked submissions
- [x] List repository urls for users/submissions

## Building

To build the tool, run the following command:

```bash
$ mvn clean package
```
in the root directory.

The jar will be at `target/artemis-cli.jar`.
