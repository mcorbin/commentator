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

Commentator supports a basic challenge system to avoid spammers.

Commentator provides an [API endpoint](/api/comments/) to return a random challenge. This API endpoint can be used to integrate Commentator on your website. What is returned by the endpoint is a json payload containing:

- A `question`, a text containing a question
- A `timestamp`
- A `signature`

When users want to create a comment, they should provide:

- The `timestamp` and the `signature` provided by the previous endpoint.
- An `answer` field containing the expected answer (case insensitive).

The `:challenges` key in the configuration can be used to configure challenges. It currently supports two modes.

### questions

```clojure
{:type :questions
 :ttl 120
 :secret #secret "azizjiuzarhuaizhaiuzr"
 :questions [{:question "1 + 4 = ?"
              :answer "5"}
             {:question "1 + 9 = ?"
              :answer "10"}]}
```

We have here two challenges with simple questions about mathematical operations. The questions and answers are totally free, it's up to you to choose what you want to ask.

You can also easily generate a lot of challenges programmatically if you want to.

When users want to create a comment, they should provide:

challenge name and the correct answer. The case of the letters in the answer is not important, everything is compared after being converted to lower case.

The TTL is the validity duration of the challenge.

### math

```clojure
{:type :math
 :ttl 120
 :secret #secret "azizjiuzarhuaizhaiuzr"}
```

This challenge will automatically generate simple mathematical challenges. It could for example return a `:question` containing `"what is the result of: 10  +  6"`

The user should, like in the `questions` challenge, provide the answer, timestamp and signature when creating a comment.

**Store**

You should put the store configuration (to access the S3 storage) in the `:store` key.

The `:bucket-prefix` value will be used as prefix for the buckets storing the comments. For example, if `:bucket-prefix` is set to "commentator-", the bucket will be "commentator-mcorbin-fr" for the `mcorbin-fr` website.

The buckets should already exist, Commentator will not create them automatically.

## Events

Every time a comment is published, an event is pushed in the website bucket in a file named `events.json`.

The [API](/api/events/) let you retrieve and delete these events. You can use this file or these endpoints to be notified when a new comment is created.

## Integration

The code I use to integrate Commentator on my personal blog is available on [Github](https://github.com/mcorbin/commentator/blob/master/integration/mcorbin/page.html).

I'm not a frontend developer. If you have frontend skills and want to write a nice integration, please reach to me on Github.
