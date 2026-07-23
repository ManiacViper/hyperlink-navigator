# Concurrent Web Crawler & Link Extractor

A concurrent producer-consumer service built with **Scala**, **FS2**, and **SBT** that fetches web pages, extracts hyperlinks, and processes results concurrently and fault-tolerantly.

---

## How to Run & Test

### Run Tests
```bash
sbt test
```

### Run App
```bash
sbt run
```

---

## Technical Assessment Requirements

### Overview
Create a concurrent producer/consumer service that fetches web pages and extracts hyperlinks. Candidates are expected to make use of AI-assisted tools where appropriate, while remaining accountable for the submission. You should understand the code, be able to explain and justify your decisions, and demonstrate clear ownership of the work submitted. Make reasonable assumptions where requirements are unclear and make those assumptions clear in your submission. This is not an all-or-nothing submission, so complete what you can. There may be an opportunity to discuss your approach in a follow-up. Please aim to submit your assessment within one week.

---

### Functional Requirements

#### The Producer
* Receives a list of URLs (e.g., file, command line, etc.).
* Extracts markup for each of the URLs and places it onto a queue.

#### The Consumer
* Reads from the queue until it is empty and the producer is no longer extracting markup.
* Parses the HTML and extracts hyperlinks.
* Outputs the extracted links (e.g., file, command line, etc.) against each parsed URL.

---

### Core Requirements

* The producer and consumer must run concurrently.
* Fetching and parsing should be fault tolerant without affecting other processing.
* Appropriate test coverage.
* Push your submission to a repository. Do not use any form of company branding or internal names in the repository name.
* Share the repository link with us.

---

### Bonus Points

* Concurrent fetching of URLs.
* Consider queue size management and behaviour under load.
* Comprehensive test coverage.
* Include any additional improvements or considerations we have neglected.

---