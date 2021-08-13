---
title: Configuration
weight: 10
disableToc: false
---

The Commentator configuration is defined in [EDN](https://github.com/edn-format/edn).
Here is a commented example configuration file:

```clojure
{;; The Mirabelle HTTP Server.
 ;; The key, cert, and cacert optional parameters can be set to enable TLS (the
 ;; parameters are path to the cert files)
 :http {:host "127.0.0.1"
        :port 8787}
 ;; The list of allowed origins for the public API.
 :allow-origin ["https://www.mcorbin.fr"]
 ;; The admin token for the admin API calls
 :admin {:token #secret "my-super-token"}
 ;; The number of minutes for an user before being able to publish another comment
 ;; on Commentator.
 ;; The `x-forwarded-for` header is first used to get the user IP
 ;; If the header is not set it fallbacks to the request source IP.
 :rate-limit-minutes 5
 :store {;; Your s3 access/secret keys
         ;; In this example the #env reader is used but you can also
         ;; specify the values without using environment variables if you
         ;; want to
         :access-key #secret #env ACCESS_KEY
         :secret-key #secret #env SECRET_KEY
         ;; the prefix for the buckets used by Commentator to store comments
         ;; and events
         :bucket-prefix "commentator-dev-"
         ;; The S3 endpoint
         :endpoint "https://sos-ch-gva-2.exo.io"}
 :comment {;; Set to true if you want to have comments automatically approved once
           ;; created
           :auto-approve false
           ;; A map containing for each website a list of articles which can receive
           ;; comments. You can use this map to disable comments for an article
           ;; for example.
           :allowed-articles {"mcorbin-fr" ["foo"
                                            "bar"]}}
 ;; Logging configuration (https://github.com/pyr/unilog)
 :logging {:level "info"
           :console {:encoder "json"}
           :overrides {:org.eclipse.jetty "info"
                       :org.apache.http "error"}}
 ;; The prometheus configuration to expose the metrics.
 ;; The key, cert, and cacert optional parameters can be set to enable TLS (the
 ;; parameters are path to the cert files)
 :prometheus {:host "127.0.0.1"
              :port 8788}
 ;; A list of challenges to avoid spammers
 :challenges {:c1 {:question "1 + 4 = ?"
                   :answer "5"}
              :c2 {:question "1 + 9 = ?"
                   :answer "10"}}}
```

Commentator can be used to store comments for multiple websites.

The `:allowed-articles` key should contain the list of articles open for comments for each website. The website names and articles should match the ones used in the [API](/api/comments/). You can also check the [use it](/howto/use-it/) section of the documentation for more information about this.

Each website will have a dedicated bucket to store its comments and events.

The bucket name will be `<bucket-prefix><sebsite-name>`. The bucket prefix should be a string between 1 and 19 characters, and the website a string between 1 and 39 characters. Allowed characters are letters (both uppercase and lowercase), number, `_` and `-`.
