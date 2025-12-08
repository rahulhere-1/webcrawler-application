# Basic Web Crawler (Educational Project)

A simple, beginner-friendly web crawler written in Python, designed purely for **learning purposes**.  
This project demonstrates core concepts like HTTP requests, HTML parsing, link extraction, recursion, and polite crawling techniques.

**Warning: This crawler is for educational use only.** Always respect `robots.txt`, rate limits, and website terms of service.

## Features

- Recursively crawls links starting from a seed URL
- Limits crawling to the same domain (avoids crawling the entire internet)
- Respects `robots.txt` (using `urllib.robotparser`)
- Configurable crawl depth
- Politeness delay (rate limiting) between requests
- Avoids crawling the same page twice (visited set)
- Saves discovered URLs and basic metadata to a file
- Simple HTML parsing with BeautifulSoup
