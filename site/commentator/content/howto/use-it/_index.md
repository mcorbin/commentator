---
title: Use it
weight: 50
disableToc: false
---

Commentator stores the articles comments on a S3 compatible store. +
Let's say we want to store comments for the blog `mcorbin.fr` for example.

The first thing to do is to write the Commentator [configuration file](/howto/configuration/).

As explained in the linked section of the documentation, several things are important.

**Allow origin**

The same Commentator instance can serve comments for several websites. You need to configure the list of websites in the `:allow-origin` section.

**Rate limit**

The `:rate-limit-minutes` option prevents users to create more than one comment every N minutes.

The `x-forwarded-for` header is first used to get the user IP. If the header is not set it fallbacks to the request source IP.

**Comment**

Comments can be automatically approved by setting the `:auto-approve` value to `true`. By default, comments not approved by an administrator are not displayed.

You also need to configure the `allowed-articles` key. It contains a map containing for each website the list of articles allowed to receive comments. For example:

```clojure
{"mcorbin-fr" ["my-first-article"
               "my-second-article"]}
```

With this setup, only requests to create comments on the `/api/v1/comment/<website>/<article>` path will be allowed, where the possible value for `<website>` is "mcorbin-fr" and the possible values for `<article>` are "my-first-article" and "my-second-article".

You can read the [API documentation](/api/comments/) to understand how it works. It's important to keep in mind that the `allowed-articles` key should match how the API is used later to store and retrieve comments for articles.

**Challenges**

Commentator supports a basic challenge system to avoid spammers. It's not perfect (and I have some idea to greatly improve it in the future) but it already helps filtering bots.

The `:challenges` key in the configuration map is a map containing for each challenge a question and a answer. For example:

```
{:c1 {:question "1 + 4 = ?"
      :answer "5"}
 :c2 {:question "1 + 9 = ?"
      :answer "10"}}
```

We have here two challenges named `:c1` and `:c2`, with simple questions about mathematical operations. The questions and answers are totally free, it's up to you to choose what you want to ask.

You can also easily generate a lot of challenges programmatically if you want to.

When users want to create a comment, they should provide a challenge name and the correct answer. The case of the letters in the answer is not important, everything is compared after being converted to lower case.

Commentator provides an [API endpoint](/api/comments/) to return a random challenge (the name and its question). This API endpoint can be used to integrate Commentator on your website.

**Store**

You should put the store configuration (to access the S3 storage) in the `:store` key.

The `:bucket-prefix` value will be used as prefix for the buckets storing the comments. For example, if `:bucket-prefix` is set to "commentator-", the bucket will be "commentator-mcorbin-fr" for the `mcorbin-fr` website.

The buckets should already exist, Commentator will not create them automatically.

## Events

Every time a comment is published, an event is pushed in the website bucket in a file named `events.json`.

The [API](/api/events/) let you retrieve and delete these events. You can use this file or these endpoints to be notified when a new comment is created.

## Integration

TODO
