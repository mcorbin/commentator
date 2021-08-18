---
title: Commentator
weight: 30
chapter: false
---

# Commentator, a free commenting system for your blog

Commentator is a simple application which provide all you need to have a powerful commenting system on your blogs or websites.

Commentator uses json files (one per article) stored on any S3-compatible storage to store your comment. The rest of the application is completely stateless.

Using a S3 compatible store as a database provide several advantages:

- Easy to use: you don't have to setup a SQL database for example.
- Highly available storage.
- You can use any S3 tools (s3cmd for example) to interact directly with your comments if you want to.

Commentator also provides:

- Multi site support: one instance of Commentator can manage comments for multiple websites.
-  An easy-to-use API to manage comments and events. A public API allows users to create or retrieve approved comments, and a admin API allows you to administrate comments (approve them or delete them for example).
Everytime a comment is added an event is generated into a dedicated file on S3. The API allows you to read and delete the events. You can use this file to be notified when a new comment is added.
- Rate limiting, either by IP or using the requests `x-forwarded-for` header.
- A "challenge" system to avoid spammers.
- An in-memory cache for comments, for performances and to avoid hitting S3 too much.
- Metrics about the applications exposed using the Prometheus format.

![Mirabelle](img/commentator_presentation.jpg)
