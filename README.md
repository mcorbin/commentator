# Commentator

A commenting system for your blog or website. Comments are stored on any S3-compatible store.

I built commentator to provide an easy to use (and easy to host) free commenting system with advanced functionalities.

## Features

Commentator provides:

- [x] Multi website support: one instance of Commentator can manage comments for multiple websites.
- [x] An easy-to-use API to manage comments and events. A public API allows users to create or retrieve approved comments, and a admin API allows you to administrate comments (approve them or delete them for example).
Everytime a comment is added an event is generated into a dedicated file on S3. The API allows you to read and delete the events. You can use this file to be notified when a new comment is added.
- [x] Rate limiting, either by IP or using the requests `x-forwarded-for` header.
- [x] A "challenge" system to avoid spammers.
- [x] An in-memory cache for comments, for performances and to avoid hitting S3 too much.
- [x] Metrics about the applications exposed using the Prometheus format.

## Documentation

The documentation is available at https://www.commentator.mcorbin.fr/
